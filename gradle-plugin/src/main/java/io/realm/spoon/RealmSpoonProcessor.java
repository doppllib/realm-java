package io.realm.spoon;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.realm.annotations.Ignore;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.Filter;
import spoon.support.reflect.code.CtVariableReadImpl;

/**
 * Created by kgalligan on 2/7/17.
 */
public class RealmSpoonProcessor extends AbstractProcessor<CtClass<?>>
{
    private Map<String, List<CtField>> classFields;

    @Override
    public void init()
    {
        List<CtClass> realmModelClasses = new ArrayList<>();
        classFields = new HashMap<>();

        for(CtElement element : getFactory().Class().getAll())
        {

            if(element instanceof CtClass && hasRealmClass((CtClass)element))
            {
                CtClass ctClass = (CtClass) element;
                System.out.println("ADDING: "+ (ctClass).getQualifiedName());
                realmModelClasses.add(ctClass);
                List<CtField> fields = ctClass.getFields();
                for(CtField field : fields)
                {
                    if(!field.hasModifier(ModifierKind.PRIVATE) && field.getAnnotation(Ignore.class) == null){
                        addField(classFields, ctClass, field);
                    }
                }
            }
        }

        for(CtClass realmModelClass : realmModelClasses)
        {
            List<CtField> fields = realmModelClass.getFields();
            for(CtField field : fields)
            {
                System.out.println("FEELDZ: "+ field.getShortRepresentation());
            }
        }
    }

    //Just messy looking
    private void addField(Map<String, List<CtField>> classFields, CtClass ctClass, CtField field)
    {
        List<CtField> ctFields = classFields.get(ctClass.getQualifiedName());
        if(ctFields == null)
        {
            ctFields = new ArrayList<>();
            classFields.put(ctClass.getQualifiedName(), ctFields);
        }
        ctFields.add(field);
    }

    private boolean hasRealmClass(CtClass ctClass)
    {
        List<CtAnnotation<? extends Annotation>> annotations = ctClass.getAnnotations();
        for(CtAnnotation<? extends Annotation> annotation : annotations)
        {
            if(annotation.getShortRepresentation().equals("@io.realm.annotations.RealmClass"))
            {
                return true;
            }
        }

        CtTypeReference<?> superclass = ctClass.getSuperclass();

        return superclass != null && superclass.getQualifiedName().equals("io.realm.RealmObject");

    }

    @Override
    public void process(CtClass<?> ctClass) {

        if(classFields.containsKey(ctClass.getQualifiedName()))
        {
            List<CtField> ctFields = classFields.get(ctClass.getQualifiedName());
            for(CtField ctField : ctFields)
            {
                {
                    CtBlock getterBlock = getFactory().Core().createBlock();
                    getterBlock.addStatement(getFactory().Code()
                            .createCodeSnippetStatement("return this." + ctField.getSimpleName()));
                    Set<ModifierKind> modifierKinds = new HashSet<>();
                    modifierKinds.add(ModifierKind.PUBLIC);
                    CtMethod ctMethod = getFactory().Method()
                            .create(ctClass,
                                    modifierKinds,
                                    ctField.getType(),
                                    "realmGet$" + ctField.getSimpleName(),
                                    null,
                                    null,
                                    getterBlock);

                    ctClass.addMethod(ctMethod);
                }

                {
                    CtBlock getterBlock = getFactory().Core().createBlock();
                    getterBlock.addStatement(getFactory().Code()
                            .createCodeSnippetStatement("this."+ ctField.getSimpleName() + " = a"));
                    Set<ModifierKind> modifierKinds = new HashSet<>();
                    modifierKinds.add(ModifierKind.PUBLIC);

                    CtMethod ctMethod = getFactory().Method()
                            .create(ctClass,
                                    modifierKinds,
                                    getFactory().Type().createReference(void.class),
                                    "realmSet$" + ctField.getSimpleName(),
                                    new ArrayList<CtParameter<?>>(),
                                    null,
                                    getterBlock);

                    getFactory().Method().createParameter(ctMethod, ctField.getType(), "a");

                    ctClass.addMethod(ctMethod);
                }
            }
        }
        else
        {
            List<CtElement> elements = ctClass.getElements(new Filter<CtElement>()
            {
                @Override
                public boolean matches(CtElement element)
                {
                    return element instanceof CtFieldRead || element instanceof CtFieldWrite;
                }
            });

            for(CtElement element : elements)
            {
                CtFieldAccess fieldAccess = (CtFieldAccess) element;

                String fieldTargetTypeName = fieldAccess.getTarget().getType().getQualifiedName();
                List<CtField> ctFields = classFields.get(fieldTargetTypeName);
                if(ctFields != null)
                {

                    System.out.println(
                            "FoundRealm: class(" + ctClass.getQualifiedName() + ") field(" +
                                    fieldAccess.getShortRepresentation() + ")");
                    for(CtField ctField : ctFields)
                    {
                        if(fieldAccess.getVariable().getSimpleName().equals(ctField.getSimpleName()))
                        {
                            if(element instanceof CtFieldRead)
                            {
                                String expression =
                                        ((CtVariableReadImpl) fieldAccess.getTarget()).getVariable()
                                                .getSimpleName() + ".realmGet$" +
                                                ctField.getSimpleName();
                                fieldAccess.replace(getFactory().Code()
                                        .createCodeSnippetExpression(expression + "()"));
                            }
                            else
                            {
                                CtAssignment ctAssignment = (CtAssignment) fieldAccess.getParent();

                                String expression =
                                        ((CtVariableReadImpl) fieldAccess.getTarget()).getVariable()
                                                .getSimpleName() + ".realmSet$" +
                                                ctField.getSimpleName();
                                String statement =
                                        expression + "(" + ctAssignment.getAssignment() + ")";

                                try
                                {
                                    ctAssignment.replace(getFactory().Code()
                                            .createCodeSnippetStatement(statement));
                                }
                                catch(Exception e)
                                {
                                    e.printStackTrace();
                                    ctAssignment.replace(getFactory().Code()
                                            .createCodeSnippetExpression(statement));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}