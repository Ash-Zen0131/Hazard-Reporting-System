import { db } from "./firebase-config.js";

import {
  collection,
  onSnapshot,
  query,
  orderBy,
  doc,
  updateDoc,
  serverTimestamp
} from "https://www.gstatic.com/firebasejs/12.6.0/firebase-firestore.js";

const hazardTableBody = document.getElementById("hazardTableBody");
const categoryFilter = document.getElementById("categoryFilter");
const searchInput = document.getElementById("searchInput");
const loadingMessage = document.getElementById("loadingMessage");
const errorMessage = document.getElementById("errorMessage");
const recordCount = document.getElementById("recordCount");

const refreshButton = document.getElementById("refreshButton");
const exportButton = document.getElementById("exportButton");

let hazardRecords = [];
let currentlyDisplayedRecords = [];

function formatDate(timestamp) {
  if (!timestamp || typeof timestamp.toDate !== "function") {
    return "No date available";
  }

  return timestamp.toDate().toLocaleString("en-MY");
}

function getStatus(hazard) {
  return hazard.status ?? "Active";
}

function getStatusClass(status) {
  return status.toLowerCase() === "resolved"
    ? "status-resolved"
    : "status-active";
}

function createMapLink(location) {
  if (!location) {
    return "#";
  }

  return `https://www.google.com/maps?q=${location.latitude},${location.longitude}`;
}

function escapeCsvValue(value) {
  const stringValue = String(value ?? "");

  return `"${stringValue.replaceAll('"', '""')}"`;
}

function displayHazards(records) {
  currentlyDisplayedRecords = records;
  hazardTableBody.innerHTML = "";

  recordCount.textContent =
    `${records.length} record${records.length === 1 ? "" : "s"} displayed`;

  if (records.length === 0) {
    hazardTableBody.innerHTML = `
      <tr>
        <td colspan="7" class="text-center py-4">
          No matching hazard records found.
        </td>
      </tr>
    `;
    return;
  }

  records.forEach((hazard) => {
    const row = document.createElement("tr");
    const status = getStatus(hazard);

    row.innerHTML = `
      <td>${hazard.userName ?? "Unknown"}</td>

      <td>
        <span class="category-badge">
          ${hazard.category ?? "Unknown"}
        </span>
      </td>

      <td class="description-cell">
        ${hazard.description ?? "-"}
      </td>

      <td>${hazard.locationName ?? "-"}</td>

      <td>
        <span class="hazard-status ${getStatusClass(status)}">
          ${status}
        </span>
      </td>

      <td>${formatDate(hazard.reportedAt)}</td>

      <td>
        <div class="d-flex gap-2 flex-wrap">

          <button
            type="button"
            class="btn btn-sm btn-outline-dark view-details-button"
            data-id="${hazard.id}"
          >
            Details
          </button>

          <a
            class="btn btn-sm btn-dark"
            href="${createMapLink(hazard.location)}"
            target="_blank"
            rel="noopener noreferrer"
          >
            Map
          </a>

          ${
            status === "Active"
              ? `
                <button
                  type="button"
                  class="btn btn-sm btn-success resolve-button"
                  data-id="${hazard.id}"
                >
                  Resolve
                </button>
              `
              : `
                <button
                  type="button"
                  class="btn btn-sm btn-secondary"
                  disabled
                >
                  Resolved
                </button>
              `
          }

        </div>
      </td>
    `;

    hazardTableBody.appendChild(row);
  });

  attachDetailsButtonEvents();
  attachResolveButtonEvents();
}

function applyFilters() {
  const selectedCategory = categoryFilter.value;
  const searchTerm = searchInput.value.trim().toLowerCase();

  const filteredRecords = hazardRecords.filter((hazard) => {
    const categoryMatches =
      selectedCategory === "All" ||
      hazard.category === selectedCategory;

    const searchableText = [
      hazard.userName,
      hazard.category,
      hazard.description,
      hazard.locationName,
      hazard.deviceModel,
      getStatus(hazard)
    ]
      .filter(Boolean)
      .join(" ")
      .toLowerCase();

    const searchMatches =
      searchTerm === "" || searchableText.includes(searchTerm);

    return categoryMatches && searchMatches;
  });

  displayHazards(filteredRecords);
}

function openDetailsModal(hazardId) {
  const hazard = hazardRecords.find(
    (record) => record.id === hazardId
  );

  if (!hazard) {
    return;
  }

  const status = getStatus(hazard);

  document.getElementById("detailUserName").textContent =
    hazard.userName ?? "Unknown";

  document.getElementById("detailCategory").textContent =
    hazard.category ?? "Unknown";

  document.getElementById("detailStatus").textContent = status;

  document.getElementById("detailDevice").textContent =
    hazard.deviceModel ?? "-";

  document.getElementById("detailLocationName").textContent =
    hazard.locationName ?? "-";

  document.getElementById("detailDescription").textContent =
    hazard.description ?? "-";

  document.getElementById("detailReportedAt").textContent =
    formatDate(hazard.reportedAt);

  const coordinates = hazard.location
    ? `${hazard.location.latitude}, ${hazard.location.longitude}`
    : "-";

  document.getElementById("detailCoordinates").textContent =
    coordinates;

  const mapLink = document.getElementById("detailMapLink");

  mapLink.href = createMapLink(hazard.location);

  if (!hazard.location) {
    mapLink.classList.add("disabled");
  } else {
    mapLink.classList.remove("disabled");
  }

  const modalElement =
    document.getElementById("hazardDetailsModal");

  const modal = bootstrap.Modal.getOrCreateInstance(modalElement);

  modal.show();
}

function attachDetailsButtonEvents() {
  const detailsButtons = document.querySelectorAll(
    ".view-details-button"
  );

  detailsButtons.forEach((button) => {
    button.addEventListener("click", () => {
      openDetailsModal(button.dataset.id);
    });
  });
}

async function resolveHazard(hazardId) {
  const confirmed = window.confirm(
    "Are you sure this hazard has been resolved?"
  );

  if (!confirmed) {
    return;
  }

  try {
    const hazardReference = doc(
      db,
      "hazards",
      hazardId
    );

    await updateDoc(hazardReference, {
      status: "Resolved",
      resolvedAt: serverTimestamp()
    });

    alert("Hazard marked as resolved.");
  } catch (error) {
    console.error("Failed to resolve hazard:", error);

    alert(
      "Unable to update the hazard status. Check Firestore rules."
    );
  }
}

function attachResolveButtonEvents() {
  const resolveButtons =
    document.querySelectorAll(".resolve-button");

  resolveButtons.forEach((button) => {
    button.addEventListener("click", () => {
      resolveHazard(button.dataset.id);
    });
  });
}

function exportToCsv() {
  if (currentlyDisplayedRecords.length === 0) {
    alert("There are no records to export.");
    return;
  }

  const header = [
    "User Name",
    "Category",
    "Description",
    "Location Name",
    "Latitude",
    "Longitude",
    "Device",
    "Status",
    "Reported Date and Time"
  ];

  const rows = currentlyDisplayedRecords.map((hazard) => [
    hazard.userName ?? "",
    hazard.category ?? "",
    hazard.description ?? "",
    hazard.locationName ?? "",
    hazard.location?.latitude ?? "",
    hazard.location?.longitude ?? "",
    hazard.deviceModel ?? "",
    getStatus(hazard),
    formatDate(hazard.reportedAt)
  ]);

  const csvContent = [
    header.map(escapeCsvValue).join(","),
    ...rows.map((row) =>
      row.map(escapeCsvValue).join(",")
    )
  ].join("\n");

  const blob = new Blob([csvContent], {
    type: "text/csv;charset=utf-8;"
  });

  const downloadUrl = URL.createObjectURL(blob);
  const temporaryLink = document.createElement("a");

  temporaryLink.href = downloadUrl;
  temporaryLink.download = "hazard-records.csv";

  document.body.appendChild(temporaryLink);
  temporaryLink.click();
  temporaryLink.remove();

  URL.revokeObjectURL(downloadUrl);
}

let unsubscribeFromHazards = null;

function startRealtimeHazardListener() {
  loadingMessage.style.display = "block";
  errorMessage.innerHTML = "";
  refreshButton.disabled = true;

  const hazardsReference = collection(db, "hazards");

  const hazardsQuery = query(
    hazardsReference,
    orderBy("reportedAt", "desc")
  );

  if (unsubscribeFromHazards) {
    unsubscribeFromHazards();
  }

  unsubscribeFromHazards = onSnapshot(
    hazardsQuery,
    (snapshot) => {
      hazardRecords = snapshot.docs.map((documentSnapshot) => ({
        id: documentSnapshot.id,
        ...documentSnapshot.data()
      }));

      applyFilters();

      loadingMessage.style.display = "none";
      refreshButton.disabled = false;
    },
    (error) => {
      console.error("Real-time hazard loading error:", error);

      loadingMessage.style.display = "none";
      refreshButton.disabled = false;

      errorMessage.innerHTML = `
        <div class="alert alert-danger">
          Unable to load hazard records.
        </div>
      `;
    }
  );
}

categoryFilter.addEventListener("change", applyFilters);
searchInput.addEventListener("input", applyFilters);
refreshButton.addEventListener("click", startRealtimeHazardListener);
exportButton.addEventListener("click", exportToCsv);

startRealtimeHazardListener();