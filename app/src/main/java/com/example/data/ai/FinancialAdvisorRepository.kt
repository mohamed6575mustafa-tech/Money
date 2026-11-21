package com.example.data.ai

import android.util.Log
import com.example.core.MoneyUtils
import com.example.domain.DashboardSnapshot
import com.example.domain.SinkingFund
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

class FinancialAdvisorRepository {
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    أنت "مستشار مصروف البيت" — مساعد مالي أسري ذكي وموثوق باللغة العربية الفصحى المبسطة.
                    مهمتك مساعدة الأسرة العربية/المصرية على فهم مصروفاتها، واكتشاف البنود المرتفعة، واقتراح خطط توفير عملية، واقتراح ميزانية الشهر التالي.
                    التزم دائماً بالأرقام الحقيقية المزودة في سياق الأسرة فقط، ولا تخترع أرقاماً من عندك.
                    إذا كانت البيانات فارغة بعد، اشرح بلطف أن عليهم البدء بتسجيل المصروفات أو الدخل وستظهر التحليلات الدقيقة فوراً.
                    اجعل إجاباتك منظمة في نقاط واضحة وعملية ومباشرة.
                    """.trimIndent()
                )
            }
        )
    }

    fun buildFinancialContext(
        snapshot: DashboardSnapshot?,
        funds: List<SinkingFund>,
        currencyCode: String
    ): String {
        if (snapshot == null) return "لا توجد بيانات ميزانية مسجلة بعد."
        val sb = StringBuilder()
        sb.appendLine("ملخص الأسرة (${snapshot.household.name}) - الشهر النشط: ${MoneyUtils.yearMonthLabelAr(snapshot.activeYearMonth)}")
        sb.appendLine("- إجمالي الدخل الشهري: ${MoneyUtils.formatCurrency(snapshot.totalIncomePiasters, currencyCode)}")
        sb.appendLine("- الميزانية الشهرية المتاحة: ${MoneyUtils.formatCurrency(snapshot.effectiveBudgetPiasters, currencyCode)}")
        sb.appendLine("- إجمالي المصروفات حتى اليوم: ${MoneyUtils.formatCurrency(snapshot.totalExpensePiasters, currencyCode)}")
        sb.appendLine("- المتبقي من الميزانية: ${MoneyUtils.formatCurrency(snapshot.remainingBudgetPiasters, currencyCode)}")
        sb.appendLine("- نسبة استهلاك الميزانية: ${snapshot.budgetUsagePercent.toInt()}%")
        sb.appendLine("- مصروفات الشهر السابق: ${MoneyUtils.formatCurrency(snapshot.previousMonthExpensePiasters, currencyCode)}")
        if (snapshot.categorySummaries.isNotEmpty()) {
            sb.appendLine("توزيع المصروفات حسب التصنيف:")
            snapshot.categorySummaries.sortedByDescending { it.spentPiasters }.forEach { cat ->
                sb.appendLine("  * ${cat.category.name}: صُرف ${MoneyUtils.formatCurrency(cat.spentPiasters, currencyCode)} من ميزانية ${MoneyUtils.formatCurrency(cat.budgetPiasters, currencyCode)} (${cat.sharePercent.toInt()}%)")
            }
        }
        if (funds.isNotEmpty()) {
            sb.appendLine("المخصصات السنوية وصناديق الادخار:")
            funds.forEach { fund ->
                sb.appendLine("  * ${fund.name}: مدخر ${MoneyUtils.formatCurrency(fund.savedPiasters, currencyCode)} من هدف ${MoneyUtils.formatCurrency(fund.targetPiasters, currencyCode)}")
            }
        }
        if (snapshot.alerts.isNotEmpty()) {
            sb.appendLine("التنبيهات الحالية:")
            snapshot.alerts.forEach { alert ->
                sb.appendLine("  ! ${alert.title}: ${alert.message}")
            }
        }
        return sb.toString()
    }

    fun streamAdvice(question: String, financialContext: String): Flow<String> = flow {
        val fullPrompt = """
            بيانات الأسرة المالية الحالية:
            $financialContext

            سؤال المستخدم:
            $question
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            model.generateContentStream(fullPrompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("لم يُرجع المساعد المالي استجابة. يرجى المحاولة مرة أخرى.")
        }
    }.catch { e ->
        if (e is CancellationException && e !is TimeoutCancellationException) {
            throw e
        }
        Log.e(TAG, "Firebase AI Advisor error", e)
        throw e
    }

    private companion object {
        const val TAG = "FinancialAdvisorRepo"
    }
}
