package org.bkkipmsemarang.kkp_scanner

import com.google.cloud.firestore.Firestore
import com.google.firebase.cloud.FirestoreClient

object FirestoreManager {
    /**
     * Returns the Firestore instance. 
     * Make sure FirebaseApp is initialized before accessing this.
     */
    val db: Firestore
        get() = FirestoreClient.getFirestore()
}
