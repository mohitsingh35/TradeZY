package com.ncs.tradezy


import java.io.Serializable

data class EachAdResponse(
    val item: EachItem? = null,
    val key: String? = ""
) : Serializable {

    data class EachItem(
        val images: List<String>? = emptyList(),
        val title: String? = "",
        val desc: String? = "",
        val price: Int? = null,
        val time: Long? = null,
        val exchangeable: String? = "",
        val buyerLocation: String? = "",
        val sellerId: String? = "",
        val viewCount: String? = "",
        val trendingViewCount: String? = "",
        val sold: String? = ""
    ) : Serializable

    constructor() : this(null, "")
}