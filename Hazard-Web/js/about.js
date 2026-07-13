import { db } from "./firebase-config.js";

import {
  collection,
  getDocs,
  limit,
  query
} from "https://www.gstatic.com/firebasejs/12.6.0/firebase-firestore.js";

const currentYearElement = document.getElementById("currentYear");
const appVersionElement = document.getElementById("appVersion");
const firebaseStatusElement = document.getElementById("firebaseStatus");
const githubLinkElement = document.getElementById("githubLink");
const copyGithubButton = document.getElementById("copyGithubButton");
const copyMessageElement = document.getElementById("copyMessage");
const openMapButton = document.getElementById("openMapButton");

currentYearElement.textContent = new Date().getFullYear();
appVersionElement.textContent = "1.0.0";

async function checkFirebaseConnection() {
  try {
    const hazardsReference = collection(db, "hazards");
    const connectionQuery = query(hazardsReference, limit(1));

    await getDocs(connectionQuery);

    firebaseStatusElement.textContent = "Connected";
    firebaseStatusElement.className = "status-connected";
  } catch (error) {
    console.error("Firebase connection check failed:", error);

    firebaseStatusElement.textContent = "Disconnected";
    firebaseStatusElement.className = "status-disconnected";
  }
}

copyGithubButton.addEventListener("click", async () => {
  const githubUrl = githubLinkElement.href;

  try {
    await navigator.clipboard.writeText(githubUrl);

    copyMessageElement.textContent = "GitHub URL copied.";
  } catch (error) {
    console.error("Unable to copy GitHub URL:", error);

    copyMessageElement.textContent =
      "Unable to copy automatically. Please copy the link manually.";
  }
});

openMapButton.addEventListener("click", () => {
  const latitude = 6.445503;
  const longitude = 100.275887;

  const googleMapsUrl =
    `https://www.google.com/maps?q=${latitude},${longitude}`;

  window.open(googleMapsUrl, "_blank", "noopener,noreferrer");
});

checkFirebaseConnection();