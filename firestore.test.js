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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validGardenPayload(uid) {
  return {
    userId: uid,
    title: "Sunlit Lavender Courtyard",
    summary: "A fragrant Mediterranean courtyard with stone paths and drought-tolerant perennials.",
    style: "Mediterranean",
    widthFeet: 24,
    lengthFeet: 18,
    sunlight: "Full Sun",
    soilType: "Sandy Loam",
    climateZone: "Zone 8-9",
    hardscapeZones: [
      {
        id: "zone_1",
        type: "pathway",
        label: "Limestone Path",
        x: 0.45,
        y: 0.1,
        width: 0.12,
        height: 0.8,
        materialNote: "Warm tumbled limestone pavers",
      },
    ],
    plants: [
      {
        id: "plant_1",
        commonName: "English Lavender",
        botanicalName: "Lavandula angustifolia",
        category: "Perennial",
        sunNeed: "Full Sun",
        waterNeed: "Low",
        matureSpreadFeet: 3.0,
        matureHeightFeet: 2.5,
        bloomSeason: "Summer",
        companionNote: "Thrives beside rosemary and olive trees",
        x: 0.25,
        y: 0.35,
        colorHex: "#7A6A9B",
        localVisualPath: "",
      },
    ],
    conceptVisualPath: "",
    createdAt: new Date(Date.now() - 1000),
    updatedAt: new Date(Date.now() - 1000),
  };
}

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

test("Unauthenticated user: cannot list or read gardens", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("gardens").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("gardens")
      .doc("g1")
      .get()
  );
});

test("Unauthenticated user: cannot create a garden", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("gardens")
      .doc("g1")
      .set(validGardenPayload(ALICE_UID))
  );
});

test("Authenticated owner: can create, read, list, update, and delete own garden", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("gardens")
    .doc("garden_1");

  await assertSucceeds(docRef.set(validGardenPayload(ALICE_UID)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("gardens").get()
  );
  await assertSucceeds(
    docRef.update({
      title: "Updated Courtyard",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(docRef.delete());
});

test("Cross-user isolation: Bob cannot read, create, update, or delete Alice's garden", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("gardens")
      .doc("garden_alice")
      .set(validGardenPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDocRef = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("gardens")
    .doc("garden_alice");

  await assertFails(aliceDocRef.get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("gardens").get()
  );
  await assertFails(aliceDocRef.set(validGardenPayload(BOB_UID)));
  await assertFails(
    aliceDocRef.update({ title: "Hijacked", updatedAt: new Date(Date.now() - 500) })
  );
  await assertFails(aliceDocRef.delete());
});

test("Shadow update & ghost field test: rejects unknown fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payload = validGardenPayload(ALICE_UID);
  payload.isAdmin = true;

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("gardens")
      .doc("garden_ghost")
      .set(payload)
  );
});

test("Identity spoofing & invalid bounds test: rejects mismatched userId and oversize title", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const spoofed = validGardenPayload(BOB_UID);

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("gardens")
      .doc("garden_spoof")
      .set(spoofed)
  );

  const oversized = validGardenPayload(ALICE_UID);
  oversized.title = "x".repeat(200);
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("gardens")
      .doc("garden_long")
      .set(oversized)
  );
});
