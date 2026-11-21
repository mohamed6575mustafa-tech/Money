const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("households").where("memberUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("householdInvites").doc("FAM123").get());
  await assertFails(
    unauthDb.collection("households").doc("hh_1").collection("transactions").doc("tx_1").set({
      householdId: "hh_1",
      type: "EXPENSE",
      amountPiasters: 5000,
      categoryId: "cat_1",
      categoryName: "بقالة",
      dateEpochMillis: Date.now(),
      yearMonth: "2026-11",
      addedByUid: ALICE_UID,
      addedByName: "أحمد",
      updatedByName: "أحمد",
      memberUids: [ALICE_UID],
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User profile: owner-only PII isolation and strict schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "أحمد",
      email: "ahmed@example.com",
      householdId: "hh_alice",
      role: "ADMIN",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user read/write blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked" }));

  // Shadow field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(1000),
    })
  );

  // Immutable createdAt rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: pastDate(100),
      updatedAt: pastDate(100),
    })
  );
});

test("Household & invite join flow + subcollection parity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastDate(10000);

  // Create household
  await assertSucceeds(
    aliceDb.collection("households").doc("hh_1").set({
      ownerId: ALICE_UID,
      name: "أسرة أحمد",
      currencyCode: "EGP",
      inviteCode: "FAM8899",
      memberUids: [ALICE_UID],
      monthlyBudgetPiasters: 1500000,
      preventBudgetOverflow: false,
      rolloverEnabled: true,
      carriedOverPiasters: 0,
      alertThreshold1: 70,
      alertThreshold2: 90,
      activeYearMonth: "2026-11",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Create invite doc
  await assertSucceeds(
    aliceDb.collection("householdInvites").doc("FAM8899").set({
      householdId: "hh_1",
      householdName: "أسرة أحمد",
      ownerId: ALICE_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invite list blocked (anti-scraping)
  await assertFails(bobDb.collection("householdInvites").get());

  // Bob can get single invite doc if he knows code
  await assertSucceeds(bobDb.collection("householdInvites").doc("FAM8899").get());

  // Bob joins household with matching inviteCode
  await assertSucceeds(
    bobDb.collection("households").doc("hh_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: pastDate(2000),
    })
  );

  // Charlie cannot read household
  await assertFails(charlieDb.collection("households").doc("hh_1").get());
  // Unfiltered household list fails
  await assertFails(aliceDb.collection("households").get());
  // Filtered household list succeeds
  await assertSucceeds(aliceDb.collection("households").where("memberUids", "array-contains", ALICE_UID).get());

  // Create category & transaction & fund & shopping & inventory
  await assertSucceeds(
    aliceDb.collection("households").doc("hh_1").collection("categories").doc("cat_grocery").set({
      householdId: "hh_1",
      name: "التموين والبقالة",
      iconKey: "shopping_basket",
      colorHex: 0xFF10B981,
      budgetPiasters: 300000,
      sortOrder: 1,
      customCategory: false,
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("households").doc("hh_1").collection("transactions").doc("tx_1").set({
      householdId: "hh_1",
      type: "EXPENSE",
      amountPiasters: 24550,
      categoryId: "cat_grocery",
      categoryName: "التموين والبقالة",
      merchant: "سوبر ماركت الخير",
      notes: "مشتريات أسبوعية",
      dateEpochMillis: Date.now() - 5000,
      yearMonth: "2026-11",
      addedByUid: ALICE_UID,
      addedByName: "أحمد",
      updatedByName: "أحمد",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial update on transaction succeeds
  await assertSucceeds(
    bobDb.collection("households").doc("hh_1").collection("transactions").doc("tx_1").update({
      amountPiasters: 25000,
      updatedByName: "سارة",
      updatedAt: pastDate(1000),
    })
  );

  // Reject extra `id` field (@DocumentId rule parity)
  await assertFails(
    aliceDb.collection("households").doc("hh_1").collection("transactions").doc("tx_2").set({
      id: "tx_2",
      householdId: "hh_1",
      type: "EXPENSE",
      amountPiasters: 1000,
      categoryId: "cat_grocery",
      categoryName: "التموين والبقالة",
      dateEpochMillis: Date.now() - 5000,
      yearMonth: "2026-11",
      addedByUid: ALICE_UID,
      addedByName: "أحمد",
      updatedByName: "أحمد",
      memberUids: [ALICE_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Filtered subcollection query succeeds; unfiltered & cross-user fail
  await assertSucceeds(
    aliceDb.collection("households").doc("hh_1").collection("transactions")
      .where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    aliceDb.collection("households").doc("hh_1").collection("transactions").get()
  );
  await assertFails(
    charlieDb.collection("households").doc("hh_1").collection("transactions").doc("tx_1").get()
  );
  await assertFails(
    charlieDb.collection("households").doc("hh_1").collection("transactions")
      .where("memberUids", "array-contains", ALICE_UID).get()
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("households").doc("hh_1").collection("funds").doc("fund_1").set({
      householdId: "hh_1",
      name: "مخصص الملابس",
      iconKey: "checkroom",
      targetPiasters: 500000,
      savedPiasters: 100000,
      targetDateMillis: Date.now() + 86400000,
      memberUids: [ALICE_UID],
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Shopping & Inventory items succeed
  await assertSucceeds(
    aliceDb.collection("households").doc("hh_1").collection("shoppingItems").doc("shop_1").set({
      householdId: "hh_1",
      name: "أرز بسمتي",
      frequency: "START_OF_MONTH",
      quantityLabel: "5 كجم",
      expectedPricePiasters: 35000,
      purchased: false,
      categoryId: "cat_grocery",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("households").doc("hh_1").collection("inventoryItems").doc("inv_1").set({
      householdId: "hh_1",
      name: "زيت طعام",
      quantity: 2.5,
      minThreshold: 1.0,
      unit: "لتر",
      expiryEpochMillis: Date.now() + 86400000 * 90,
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
