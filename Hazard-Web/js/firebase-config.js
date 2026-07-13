// Import Firebase
import { initializeApp } from "https://www.gstatic.com/firebasejs/12.6.0/firebase-app.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/12.6.0/firebase-firestore.js";

// Your Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyAjboLB9pQT2E3-miy-eNCfTx7vl5p7Go8",
  authDomain: "known-hazard-reporting-s-25b6d.firebaseapp.com",
  projectId: "known-hazard-reporting-s-25b6d",
  storageBucket: "known-hazard-reporting-s-25b6d.firebasestorage.app",
  messagingSenderId: "464300573063",
  appId: "1:464300573063:web:5856c5bc0c71444b5c1322"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize Firestore
const db = getFirestore(app);

// Export Firestore so other JavaScript files can use it
export { db };