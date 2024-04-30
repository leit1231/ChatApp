package com.example.chatapp

class Message {
    var message: String? = null
    var senderId: String? = null
    var receiverUid: String? = null
    var timestamp: Long = System.currentTimeMillis()

    constructor(){}

    constructor(message: String?, senderId: String?, timestamp: Long?, receiverUid: String?){
        this.message = message
        this.senderId = senderId
        if (timestamp != null) {
            this.timestamp = timestamp
        }
        this.receiverUid = receiverUid
    }

}