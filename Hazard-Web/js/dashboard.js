import { db } from "./firebase-config.js";

import {
  collection,
  getDocs,
  query,
  orderBy,
  limit
} from "https://www.gstatic.com/firebasejs/12.6.0/firebase-firestore.js";

const totalHazardsElement = document.getElementById("totalHazards");
const roadHazardsElement = document.getElementById("roadHazards");
const environmentalHazardsElement = document.getElementById(
  "environmentalHazards"
);
const buildingHazardsElement = document.getElementById("buildingHazards");

const hazardListElement = document.getElementById("hazardList");
const loadingMessageElement = document.getElementById("loadingMessage");
const errorMessageElement = document.getElementById("errorMessage");

const firebaseStatusElement = document.getElementById("firebaseStatus");
const systemConnectionElement = document.getElementById("systemConnection");
const lastSyncElement = document.getElementById("lastSync");

function formatDate(timestamp) {
  if (!timestamp) {
    return "No date available";
  }

  return timestamp.toDate().toLocaleString("en-MY");
}

function getCategoryClass(category) {
  if (category === "Road Hazard") {
    return "category-road";
  }

  if (category === "Environmental Hazard") {
    return "category-environment";
  }

  if (category === "Building Hazard") {
    return "category-building";
  }

  return "category-default";
}

async function loadDashboard() {
  try {
    const hazardsReference = collection(db, "hazards");

    const allHazardsQuery = query(
      hazardsReference,
      orderBy("reportedAt", "desc")
    );

    const recentHazardsQuery = query(
      hazardsReference,
      orderBy("reportedAt", "desc"),
      limit(5)
    );

    const [allSnapshot, recentSnapshot] = await Promise.all([
      getDocs(allHazardsQuery),
      getDocs(recentHazardsQuery)
    ]);

    let road = 0;
    let environmental = 0;
    let building = 0;

    allSnapshot.forEach((documentSnapshot) => {
      const hazard = documentSnapshot.data();

      if (hazard.category === "Road Hazard") {
        road++;
      }

      if (hazard.category === "Environmental Hazard") {
        environmental++;
      }

      if (hazard.category === "Building Hazard") {
        building++;
      }
    });

    totalHazardsElement.textContent = allSnapshot.size;
    roadHazardsElement.textContent = road;
    environmentalHazardsElement.textContent = environmental;
    buildingHazardsElement.textContent = building;

    hazardListElement.innerHTML = "";

    recentSnapshot.forEach((documentSnapshot) => {
      const hazard = documentSnapshot.data();

      const item = document.createElement("article");
      item.className = "recent-item";

      item.innerHTML = `
        <div class="recent-item-top">
          <span class="category-badge ${getCategoryClass(hazard.category)}">
            ${hazard.category ?? "Unknown Category"}
          </span>

          <span class="recent-date">
            ${formatDate(hazard.reportedAt)}
          </span>
        </div>

        <h3>${hazard.userName ?? "Unknown User"}</h3>

        <p>${hazard.description ?? "No description provided."}</p>

        <div class="recent-location">
          📍 ${hazard.locationName ?? "Unknown location"}
        </div>
      `;

      hazardListElement.appendChild(item);
    });

    if (recentSnapshot.empty) {
      hazardListElement.innerHTML =
        "<p>No hazard reports are available.</p>";
    }

    loadingMessageElement.style.display = "none";

    firebaseStatusElement.textContent = "Connected";
    systemConnectionElement.textContent = "Online";

    lastSyncElement.textContent = new Date().toLocaleString("en-MY");
  } catch (error) {
    console.error("Dashboard loading error:", error);

    loadingMessageElement.style.display = "none";

    errorMessageElement.innerHTML = `
      <div class="alert alert-danger">
        Unable to load hazard data.
      </div>
    `;

    firebaseStatusElement.textContent = "Disconnected";
    systemConnectionElement.textContent = "Offline";
  }
}

loadDashboard();