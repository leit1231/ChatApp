package com.example.chatapp

class User {
    var name: String? = null
    var email: String? = null
    var uid: String? = null
    var profileImage: String? = null
    var lastMessage: String? = null
    var userCountry: String? = null
    var selectedCountry: String? = null


    constructor(){}

    constructor(name: String?, email: String?, uid: String?, profileImage: String?, lastMessage: String?, userCountry: String?, selectedCountry: String?){
        this.name = name
        this.email = email
        this.uid = uid
        this.profileImage = profileImage
        this.lastMessage = lastMessage
        this.userCountry = userCountry
        this.selectedCountry = selectedCountry
    }
}