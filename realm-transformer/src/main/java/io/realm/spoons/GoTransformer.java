package io.realm.spoons;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by kgalligan on 2/1/17.
 */
public class GoTransformer extends AbstractProcessor<CtClass<?>> {
    @Override
    public void process(CtClass<?> ctClass) {
        final CtTypeReference<Date> dateRef = getFactory().Code().createCtTypeReference(Date.class);
        final CtTypeReference<List<Date>> listRef = getFactory().Code().createCtTypeReference(List.class);
        listRef.addActualTypeArgument(dateRef);
        final CtField<List<Date>> listOfDates = getFactory().Core().<List<Date>>createField();
        listOfDates.<CtField>setType(listRef);
        listOfDates.<CtField>addModifier(ModifierKind.PRIVATE);
        listOfDates.setSimpleName("dates");

        // Creates constructor.
        final CtCodeSnippetStatement statementInConstructor = getFactory().Code().createCodeSnippetStatement("this.dates = dates");
        final CtBlock<?> ctBlockOfConstructor = getFactory().Code().createCtBlock(statementInConstructor);
        final CtParameter<List<Date>> parameter = getFactory().Core().<List<Date>>createParameter();
        parameter.<CtParameter>setType(listRef);
        parameter.setSimpleName("dates");
        final CtConstructor constructor = getFactory().Core().createConstructor();
        constructor.setBody(ctBlockOfConstructor);
        constructor.setParameters(Collections.<CtParameter<?>>singletonList(parameter));
        constructor.addModifier(ModifierKind.PUBLIC);

        // Apply transformation.
        ctClass.addField(listOfDates);
        ctClass.addConstructor(constructor);
    }
}
