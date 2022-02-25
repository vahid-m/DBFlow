package com.dbflow5.content

import android.net.Uri
import com.dbflow5.TABLE_QUERY_PARAM
import com.dbflow5.query.NameAlias
import com.dbflow5.query.operations.Operation
import com.dbflow5.query.operations.operator
import com.dbflow5.structure.ChangeAction

interface ContentNotificationDecoder {

    fun decode(uri: Uri): ContentNotification?
}

interface ContentNotificationEncoder {

    fun encode(contentNotification: ContentNotification): Uri
}

fun defaultContentDecoder(): ContentNotificationDecoder =
    DefaultContentNotificationDecoder

fun defaultContentEncoder(): ContentNotificationEncoder = DefaultContentNotificationEncoder

internal object DefaultContentNotificationDecoder : ContentNotificationDecoder {
    override fun decode(uri: Uri): ContentNotification? {
        val tableName = uri.getQueryParameter(TABLE_QUERY_PARAM)

        if (tableName != null) {
            val fragment = uri.fragment
            val queryNames = uri.queryParameterNames
            val columns = queryNames.asSequence()
                .filter { it != TABLE_QUERY_PARAM }
                .map { key ->
                    val param = Uri.decode(uri.getQueryParameter(key))
                    val columnName = Uri.decode(key)
                    operator(
                        NameAlias.Builder(columnName).build(),
                        operation = Operation.Equals,
                        param
                    )
                }.toList()
            val action = fragment?.let { ChangeAction.valueOf(it) } ?: ChangeAction.NONE

            // model level change when we have column names in Uri
            return if (columns.isNotEmpty()) {
                ContentNotification.ModelChange<Any>(
                    tableName = tableName,
                    action = action,
                    authority = uri.authority ?: "",
                    changedFields = columns,
                )
            } else {
                ContentNotification.TableChange(
                    tableName = tableName,
                    action = action,
                    authority = uri.authority ?: "",
                )
            }
        } else {
            return null
        }
    }
}

internal object DefaultContentNotificationEncoder : ContentNotificationEncoder {
    override fun encode(contentNotification: ContentNotification): Uri =
        Uri.Builder().scheme("dbflow")
            .authority(contentNotification.authority)
            .appendQueryParameter(TABLE_QUERY_PARAM, contentNotification.tableName)
            .apply {
                if (contentNotification.action != ChangeAction.NONE) fragment(
                    contentNotification.action.name
                )
                if (contentNotification is ContentNotification.ModelChange<*>) {
                    contentNotification.changedFields.forEach { operator ->
                        appendQueryParameter(
                            operator.nameAlias.query,
                            operator.value.toString(),
                        )
                    }
                }
            }
            .build()
}