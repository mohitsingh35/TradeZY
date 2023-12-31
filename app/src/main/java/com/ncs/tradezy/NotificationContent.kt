package com.ncs.tradezy

data class NotificationContent(
    val item: NotificationItem?,
    val key: String?="",

    ){
    data class NotificationItem(
        val title:String?="",
        val message:String?="",
        val time:Long?=null,
        val receiverID:String?="",
        val senderID:String?="",
        val read:String?="",
        val ad:EachAdResponse?=null,
        val msgread:Map<String,String>?= emptyMap(),
        val senderurl:String?="",
        val sendername:String?="",
    )
}
