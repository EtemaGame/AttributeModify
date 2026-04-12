package com.etema.attributemodify;

import com.etema.attributemodify.util.AttributeTooltipHelper;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AttributeTooltipHelperTest {

    @Test
    public void testFormatNumber() {
        assertEquals("10", AttributeTooltipHelper.formatNumber(10.0));
        assertEquals("10.5", AttributeTooltipHelper.formatNumber(10.5));
        assertEquals("0.05", AttributeTooltipHelper.formatNumber(0.05));
        assertEquals("0.1", AttributeTooltipHelper.formatNumber(0.1));
    }

    @Test
    public void testGetTranslationKey() {
        // Addition (Op 0)
        assertEquals("attribute.modifier.plus.0", 
            AttributeTooltipHelper.getTranslationKey(AttributeModifier.Operation.ADDITION, true, false));
        assertEquals("attribute.modifier.take.0", 
            AttributeTooltipHelper.getTranslationKey(AttributeModifier.Operation.ADDITION, false, false));
        assertEquals("attribute.modifier.equals.0", 
            AttributeTooltipHelper.getTranslationKey(AttributeModifier.Operation.ADDITION, true, true));

        // Multiply Base (Op 1)
        assertEquals("attribute.modifier.plus.1", 
            AttributeTooltipHelper.getTranslationKey(AttributeModifier.Operation.MULTIPLY_BASE, true, false));
        
        // Multiply Total (Op 2)
        assertEquals("attribute.modifier.plus.2", 
            AttributeTooltipHelper.getTranslationKey(AttributeModifier.Operation.MULTIPLY_TOTAL, true, false));
    }

    @Test
    public void testComputeDisplayValue() {
        // Addition, not new attribute
        assertEquals(15.0, AttributeTooltipHelper.computeDisplayValue(15.0, 5.0, AttributeModifier.Operation.ADDITION, false, false));
        
        // Addition, new attribute
        assertEquals(5.0, AttributeTooltipHelper.computeDisplayValue(15.0, 5.0, AttributeModifier.Operation.ADDITION, true, false));
        
        // Multiplication (always shows percentage of modifier)
        assertEquals(50.0, AttributeTooltipHelper.computeDisplayValue(15.0, 0.5, AttributeModifier.Operation.MULTIPLY_BASE, false, false));
    }
}
