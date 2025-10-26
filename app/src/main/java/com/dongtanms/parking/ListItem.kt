package com.dongtanms.parking

sealed class ListItem {
    data class EntryItem(val parkingEntry: ParkingEntry) : ListItem()
    data class DateItem(val date: String) : ListItem()

    val id: String
        get() = when (this) {
            is EntryItem -> parkingEntry.id.toString()
            is DateItem -> date
        }
}
