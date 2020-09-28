package com.probill.model

import java.sql.Timestamp

abstract class Base {
    var id: Long = 0
    var createdAt: Timestamp = Timestamp(0)
    var updatedAt: Timestamp = Timestamp(0)
}