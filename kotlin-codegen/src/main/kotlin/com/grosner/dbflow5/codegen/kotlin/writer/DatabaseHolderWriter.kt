package com.grosner.dbflow5.codegen.kotlin.writer

import com.dbflow5.codegen.shared.ClassAdapterFieldModel.Type
import com.dbflow5.codegen.shared.ClassModel
import com.dbflow5.codegen.shared.ClassNames
import com.dbflow5.codegen.shared.DatabaseHolderModel
import com.dbflow5.codegen.shared.GeneratedClassModel
import com.dbflow5.codegen.shared.interop.OriginatingFileTypeSpecAdder
import com.dbflow5.codegen.shared.properties.nameWithFallback
import com.dbflow5.codegen.shared.writer.TypeCreator
import com.dbflow5.stripQuotes
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Description:
 */
class DatabaseHolderWriter(
    private val originatingFileTypeSpecAdder: OriginatingFileTypeSpecAdder,
) : TypeCreator<DatabaseHolderModel, FileSpec> {

    private val nameAllocator = NameAllocator()

    override fun create(model: DatabaseHolderModel): FileSpec {
        return FileSpec.builder(
            model.name.packageName,
            model.properties.nameWithFallback(
                model.name.shortName,
            ).stripQuotes()
        ).addType(
            TypeSpec.classBuilder(
                model.properties.nameWithFallback(
                    model.name.shortName,
                ).stripQuotes()
            )
                .apply {
                    originatingFileTypeSpecAdder.addOriginatingFileType(
                        this,
                        model.originatingSource,
                    )
                }
                .addSuperinterface(ClassNames.DatabaseHolderFactory)
                .addClassProperties(model.tables)
                .addClassProperties(model.queries)
                .addClassProperties(model.views)
                .addDatabaseProperties(model)
                .addFunction(
                    FunSpec.builder("create")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(ClassNames.DatabaseHolder)
                        .addCode("return %T(\n", ClassNames.DatabaseHolder)
                        .addSuperGeneratedSet("databases", model.databases)
                        .addSuperGeneratedSet("tables", model.tables)
                        .addSuperGeneratedSet("views", model.views)
                        .addSuperGeneratedSet("queries", model.queries)
                        .addCode(")\n")
                        .build()
                )

                .build()
        )
            .build()
    }

    private fun FunSpec.Builder.addSuperGeneratedSet(
        name: String,
        objects: List<GeneratedClassModel>,
    ) = apply {
        addCode(
            "$name = setOf(${objects.joinToString { nameAllocator[it.generatedClassName] }}),\n",
        )
    }

    private fun TypeSpec.Builder.addDatabaseProperties(
        model: DatabaseHolderModel,
    ) = apply {
        model.databases.forEach { db ->
            val name =
                nameAllocator.newName(
                    db.generatedClassName.shortName.stripQuotes()
                        .replaceFirstChar { it.lowercase() },
                    db.generatedClassName,
                )
            addProperty(
                PropertySpec.builder(
                    name,
                    ClassNames.DBFlowDatabase,
                )
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(
                        "%T(${db.adapterFields.joinToString { "%L = %N" }})",
                        db.generatedClassName.className,
                        *db.adapterFields
                            .map { it.name.shortName }.toTypedArray(),
                        *db.adapterFields
                            .map { fieldModel ->
                                when (fieldModel.type) {
                                    Type.Normal -> model.tables.first { it.classType == fieldModel.modelType }
                                    Type.Query -> model.queries.first { it.classType == fieldModel.modelType }
                                    Type.View -> model.views.first { it.classType == fieldModel.modelType }
                                }
                            }
                            .map { nameAllocator[it.generatedClassName] }.toTypedArray(),
                    )
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.addClassProperties(
        objects: List<ClassModel>,
    ) = apply {
        objects.forEach { obj ->
            val name =
                nameAllocator.newName(
                    obj.dbName.stripQuotes().replaceFirstChar { it.lowercase() },
                    obj.generatedClassName
                )
            addProperty(
                PropertySpec.builder(
                    name,
                    obj.generatedSuperClass,
                )
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("%T()", obj.generatedClassName.className)
                    .build()
            )
        }
    }
}