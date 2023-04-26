package com.willfp.eco.internal.spigot.math

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.willfp.eco.core.integrations.placeholder.PlaceholderManager
import com.willfp.eco.core.placeholder.context.PlaceholderContext
import redempt.crunch.CompiledExpression
import redempt.crunch.Crunch
import redempt.crunch.data.FastNumberParsing
import redempt.crunch.functional.EvaluationEnvironment
import redempt.crunch.functional.Function
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

private val goToZero = Crunch.compileExpression("0")

private val min = Function("min", 2) {
    min(it[0], it[1])
}

private val max = Function("max", 2) {
    max(it[0], it[1])
}

interface ExpressionHandler {
    fun evaluate(expression: String, context: PlaceholderContext): Double
}

class ImmediatePlaceholderTranslationExpressionHandler : ExpressionHandler {
    private val cache: Cache<String, CompiledExpression> = Caffeine.newBuilder()
        .expireAfterAccess(500, TimeUnit.MILLISECONDS)
        .build()

    private val env = EvaluationEnvironment().apply {
        addFunctions(min, max)
    }

    override fun evaluate(expression: String, context: PlaceholderContext): Double {
        val translatedExpression = PlaceholderManager.translatePlaceholders(expression, context)

        val compiled = cache.get(translatedExpression) {
            runCatching { Crunch.compileExpression(translatedExpression, env) }
                .getOrDefault(goToZero)
        }

        return runCatching { compiled.evaluate() }.getOrDefault(0.0)
    }
}

class LazyPlaceholderTranslationExpressionHandler : ExpressionHandler {
    private val cache: Cache<String, CompiledExpression> = Caffeine.newBuilder()
        .build()

    override fun evaluate(expression: String, context: PlaceholderContext): Double {
        val placeholders = PlaceholderManager.findPlaceholdersIn(expression)

        val placeholderValues = placeholders
            .map { PlaceholderManager.translatePlaceholders(it, context) }
            .map { runCatching { FastNumberParsing.parseDouble(it) }.getOrDefault(0.0) }
            .toDoubleArray()

        val compiled = cache.get(expression) {
            val env = EvaluationEnvironment()
            env.setVariableNames(*placeholders.toTypedArray())
            env.addFunctions(min, max)
            runCatching { Crunch.compileExpression(expression, env) }.getOrDefault(goToZero)
        }

        return runCatching { compiled.evaluate(*placeholderValues) }.getOrDefault(0.0)
    }
}