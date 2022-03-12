package com.dbflow5.database

import co.touchlab.sqliter.DatabaseConnection
import co.touchlab.sqliter.getVersion
import com.dbflow5.config.GeneratedDatabase
import kotlinx.atomicfu.atomic

class NativeDatabase(
    override val generatedDatabase: GeneratedDatabase,
    internal val db: DatabaseConnection,
) : DatabaseWrapper {

    private var inTransaction by atomic(false)

    override val isInTransaction: Boolean
        get() = inTransaction
    override val version: Int
        get() = db.getVersion()

    override fun execSQL(query: String) {
        db.createStatement(query).execute()
    }

    override suspend fun <R> executeTransaction(dbFn: suspend DatabaseWrapper.() -> R): R {
        // only allow a single transaction to occur.
        val wasInTransaction = inTransaction
        try {
            if (!wasInTransaction) {
                db.beginTransaction()
                inTransaction = true
            }
            val result = dbFn()
            if (!wasInTransaction) {
                db.setTransactionSuccessful()
            }
            return result
        } finally {
            if (!wasInTransaction) {
                db.endTransaction()
            }
        }
    }

    override fun compileStatement(rawQuery: String): DatabaseStatement =
        NativeDatabaseStatement(db.createStatement(rawQuery))

    override fun rawQuery(query: String): FlowCursor =
        NativeFlowCursor(db.createStatement(query).query())

    override val isOpen: Boolean
        get() = !db.closed
}

fun co.touchlab.sqliter.interop.SQLiteException.toDBFlowSQLiteException() =
    SQLiteException("A Database Error Occurred", this)

inline fun <T> rethrowDBFlowException(fn: () -> T) = try {
    fn()
} catch (e: co.touchlab.sqliter.interop.SQLiteException) {
    throw e.toDBFlowSQLiteException()
}
