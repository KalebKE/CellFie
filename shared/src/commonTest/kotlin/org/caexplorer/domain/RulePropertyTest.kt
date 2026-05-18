package org.caexplorer.domain

import org.caexplorer.domain.rule.Rule
import org.caexplorer.domain.rule.RuleProperty
import org.caexplorer.domain.rule.implementations.Life
import org.caexplorer.domain.rule.implementations.Wireworld
import org.caexplorer.domain.rule.implementations.WolframRule
import kotlin.test.*

class RulePropertyTest {

    @Test
    fun wolframRuleExposesRuleNumberProperty() {
        val rule = WolframRule(30)
        val props = rule.properties
        assertEquals(1, props.size)

        val prop = props[0]
        assertTrue(prop is RuleProperty.IntProperty)
        assertEquals("ruleNumber", prop.key)
        assertEquals(30, (prop as RuleProperty.IntProperty).value)
        assertEquals(0, prop.min)
        assertEquals(255, prop.max)
    }

    @Test
    fun wolframRuleWithPropertyCreatesNewRule() {
        val rule30 = WolframRule(30)
        val rule110 = rule30.withProperty("ruleNumber", 110)

        assertNotSame(rule30, rule110)
        assertTrue(rule110 is WolframRule)
        assertEquals(110, (rule110 as WolframRule).ruleNumber)
        assertEquals("Rule 110", rule110.displayName)
    }

    @Test
    fun ruleDefaultPropertiesIsEmptyList() {
        // Wireworld uses the Rule interface default (empty properties)
        val rule = Wireworld()
        assertTrue(rule.properties.isEmpty())
    }

    @Test
    fun ruleDefaultWithPropertyReturnsSelf() {
        // Wireworld uses the Rule interface default (returns self)
        val rule = Wireworld()
        val result = rule.withProperty("unknown", 42)
        assertSame(rule, result)
    }
}
