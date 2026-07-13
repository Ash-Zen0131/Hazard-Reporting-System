import { db } from "./firebase-config.js";

import {
  collection,
  getDocs,
  query,
  orderBy
} from "https://www.gstatic.com/firebasejs/12.6.0/firebase-firestore.js";

const totalReportsElement = document.getElementById("totalReports");
const totalLocationsElement = document.getElementById("totalLocations");
const latestReportElement = document.getElementById("latestReport");
const mostCommonCategoryElement =
  document.getElementById("mostCommonCategory");

const loadingMessageElement = document.getElementById("loadingMessage");
const errorMessageElement = document.getElementById("errorMessage");

function formatDate(timestamp) {
  if (!timestamp) {
    return "-";
  }

  return timestamp.toDate().toLocaleDateString("en-MY", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  });
}

function findMostCommonCategory(counts) {
  const entries = Object.entries(counts);

  if (entries.length === 0) {
    return "-";
  }

  entries.sort((first, second) => second[1] - first[1]);

  return entries[0][1] === 0 ? "-" : entries[0][0];
}

function createCharts(counts) {
  const labels = [
    "Road Hazard",
    "Environmental Hazard",
    "Building Hazard"
  ];

  const values = [
    counts["Road Hazard"],
    counts["Environmental Hazard"],
    counts["Building Hazard"]
  ];

  new Chart(document.getElementById("categoryPieChart"), {
    type: "pie",
    data: {
      labels,
      datasets: [
        {
          data: values,
          backgroundColor: [
            "#dc3545",
            "#198754",
            "#0d6efd"
          ]
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false
    }
  });

  new Chart(document.getElementById("categoryBarChart"), {
    type: "bar",
    data: {
      labels,
      datasets: [
        {
          label: "Number of Reports",
          data: values,
          backgroundColor: [
            "#dc3545",
            "#198754",
            "#0d6efd"
          ]
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            precision: 0
          }
        }
      }
    }
  });
}

async function loadStatistics() {
  try {
    const hazardsReference = collection(db, "hazards");

    const hazardsQuery = query(
      hazardsReference,
      orderBy("reportedAt", "desc")
    );

    const snapshot = await getDocs(hazardsQuery);

    const counts = {
      "Road Hazard": 0,
      "Environmental Hazard": 0,
      "Building Hazard": 0
    };

    const uniqueLocations = new Set();

    snapshot.forEach((documentSnapshot) => {
      const hazard = documentSnapshot.data();

      if (counts[hazard.category] !== undefined) {
        counts[hazard.category]++;
      }

      if (hazard.locationName) {
        uniqueLocations.add(hazard.locationName.trim().toLowerCase());
      }
    });

    totalReportsElement.textContent = snapshot.size;
    totalLocationsElement.textContent = uniqueLocations.size;

    const latestDocument = snapshot.docs[0];

    latestReportElement.textContent = latestDocument
      ? formatDate(latestDocument.data().reportedAt)
      : "-";

    mostCommonCategoryElement.textContent =
      findMostCommonCategory(counts);

    createCharts(counts);

    loadingMessageElement.style.display = "none";
  } catch (error) {
    console.error("Statistics loading error:", error);

    loadingMessageElement.style.display = "none";

    errorMessageElement.innerHTML = `
      <div class="alert alert-danger">
        Unable to load statistics.
      </div>
    `;
  }
}

loadStatistics();