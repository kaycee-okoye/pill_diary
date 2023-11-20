package com.oaktech.pilldiary

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


/**
 * Data class for storing medication data
 *
 * @param name the name of the medication
 * @param dosage the dosage of the medication
 * @param id rowid of the pill in sqlite database
 */
class Pill(var name: String, var dosage: String, var id: Int?) : Comparable<Pill> {
    /**
     * Method to compare this object with another Pill object
     *
     * @param other pill object to compare with
     * @return 0 if they have the same name, a positive/negative number
     * otherwise. Comparing is case-insensitive
     */
    override fun compareTo(other: Pill): Int {
        return this.name.lowercase()
            .compareTo(other.name.lowercase())
    }

    /**
     * Method to convert the medication object to a string
     *
     * @return string representation of the Pill object
     */
    override fun toString(): String {
        return "$name,$dosage"
    }
}

/**
 * Data class for storing medication journal entry data
 */
class Entry : Comparable<Entry> {
    var isPillAdministeration: Boolean = false // whether the entry was automatically

    // generated for by a medication being administered
    var entryDate: LocalDateTime? = null // date entry was made
    var entryMessage: String = "" // the message associated with this entry
    var id: Int? = null // rowid of the entry in sqlite database
    private val DATE_FORMAT: String = "EEE dd/MM/yyyy mm:KK a" // date format used
    // to convert LocalDateTime to and from string

    /**
     * Empty constructor for Entry class
     */
    constructor() {
        this.entryMessage = ""
        this.entryDate = LocalDateTime.now()
    }

    /**
     * Constructor for Entry class
     *
     * @param entryDate date entry was made
     * @param entryMessage the message associated with this entry
     */
    constructor(entryDate: LocalDateTime, entryMessage: String) {
        this.entryDate = entryDate
        this.entryMessage = entryMessage
    }

    /**
     * Constructor for Entry class
     *
     * @param entryDate date entry was made in string format
     * @param entryMessage the message associated with this entry
     */
    constructor(entryDateString: String, entryMessage: String) {
        val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.getDefault())
        this.entryDate = LocalDateTime.parse(entryDateString, formatter)
        this.entryMessage = entryMessage
    }

    /**
     * Constructor for Entry class
     *
     * This constructor is for automatically generating entry from pill administered
     *
     * @param pill pill object that was administered
     */
    constructor(pill: Pill) {
        this.entryMessage = "${pill.name} (${pill.dosage}) was administered"
        this.entryDate = LocalDateTime.now()
        this.isPillAdministeration = true
    }

    /**
     * Method to convert the entry object to a string
     *
     * @return string representation of the entry object
     */
    override fun toString(): String {
        return "$entryDate, $entryMessage"
    }

    /**
     * Method to compare this object with another Entry object
     *
     * @param other Entry object to compare with
     *
     * @return -1 if this entry was made before the other entry object,
     * 0 if this entry was made at the same time as the other entry object,
     * 1 if this entry was made after the other entry object
     */
    override fun compareTo(other: Entry): Int {
        if (this.entryDate == null) {
            return 1
        } else if (other.entryDate == null) {
            return -1
        } else {
            return this.entryDate!!.compareTo(other.entryDate)
        }
    }

    /**
     * Method to format entryDate
     *
     * @return formatted date, returns empty string
     * if entryDate is null
     */
    fun displayDate(): String {
        if (this.entryDate == null) {
            return ""
        }
        val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.getDefault())
        return this.entryDate!!.format(formatter)
    }
}