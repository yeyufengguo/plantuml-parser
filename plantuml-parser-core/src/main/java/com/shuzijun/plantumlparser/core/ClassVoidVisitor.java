package com.shuzijun.plantumlparser.core;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * 类
 *
 * @author shuzijun
 */
public class ClassVoidVisitor extends VoidVisitorAdapter<PUmlView> {

    private final String packageName;

    private final ParserConfig parserConfig;

    public ClassVoidVisitor(String packageName, ParserConfig parserConfig) {
        this.packageName = packageName;
        this.parserConfig = parserConfig;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration cORid, PUmlView pUmlView) {
        PUmlClass pUmlClass = new PUmlClass();
        pUmlClass.setPackageName(packageName);
        pUmlClass.setClassName(cORid.getNameAsString());
        if (cORid.isInterface()) {
            pUmlClass.setClassType("interface");
        } else {
            pUmlClass.setClassType("class");
            for (Modifier modifier : cORid.getModifiers()) {
                if (modifier.toString().trim().contains("abstract")) {
                    pUmlClass.setClassType("abstract class");
                    break;
                }
            }
        }
        for (FieldDeclaration field : cORid.getFields()) {
            PUmlField pUmlField = new PUmlField();
            if (field.getModifiers().size() != 0) {
                for (Modifier modifier : field.getModifiers()) {
                    if (VisibilityUtils.isVisibility(modifier.toString().trim())) {
                        pUmlField.setVisibility(modifier.toString().trim());
                        break;
                    }
                }
            }
            if (parserConfig.isFieldModifier(pUmlField.getVisibility())) {
                pUmlField.setStatic(field.isStatic());
                pUmlField.setType(field.getVariables().getFirst().get().getTypeAsString());
                pUmlField.setName(field.getVariables().getFirst().get().getNameAsString());
                pUmlClass.addPUmlFieldList(pUmlField);
            }

        }
        for (MethodDeclaration method : cORid.getMethods()) {
            PUmlMethod pUmlMethod = new PUmlMethod();

            if (method.getModifiers().size() != 0) {
                for (Modifier modifier : method.getModifiers()) {
                    if (VisibilityUtils.isVisibility(modifier.toString().trim())) {
                        pUmlMethod.setVisibility(modifier.toString().trim());
                        break;
                    }
                }
            }
            if (parserConfig.isMethodModifier(pUmlMethod.getVisibility())) {
                pUmlMethod.setStatic(method.isStatic());
                pUmlMethod.setAbstract(method.isAbstract());
                pUmlMethod.setReturnType(method.getTypeAsString());
                pUmlMethod.setName(method.getNameAsString());
                for (Parameter parameter : method.getParameters()) {
                    pUmlMethod.addParam(parameter.getTypeAsString());
                }
                pUmlClass.addPUmlMethodList(pUmlMethod);
            }
        }
        pUmlView.addPUmlClass(pUmlClass);

        Node node = cORid.getParentNode().get();

        NodeList<ImportDeclaration> importDeclarations = parseImport(node, pUmlClass, pUmlView);

        Map<String, String> importMap = new HashMap<>();
        if (importDeclarations != null) {
            for (ImportDeclaration importDeclaration : importDeclarations) {
                importMap.put(importDeclaration.getName().getIdentifier(), importDeclaration.getName().toString());
            }
        }
        if (cORid.getImplementedTypes().size() != 0) {
            for (ClassOrInterfaceType implementedType : cORid.getImplementedTypes()) {
                PUmlRelation pUmlRelation = new PUmlRelation();
                pUmlRelation.setTarget(pUmlClass.getPackageName() + "." + pUmlClass.getClassName());
                if (importMap.containsKey(implementedType.getNameAsString())) {
                    pUmlRelation.setSource(importMap.get(implementedType.getNameAsString()));
                } else {
                    pUmlRelation.setSource(pUmlClass.getPackageName() + "." + implementedType.getNameAsString());
                }
                pUmlRelation.setRelation("<|..");
                pUmlView.addPUmlRelation(pUmlRelation);
            }
        }

        if (cORid.getExtendedTypes().size() != 0) {
            for (ClassOrInterfaceType extendedType : cORid.getExtendedTypes()) {
                PUmlRelation pUmlRelation = new PUmlRelation();
                pUmlRelation.setTarget(pUmlClass.getPackageName() + "." + pUmlClass.getClassName());
                if (importMap.containsKey(extendedType.getNameAsString())) {
                    pUmlRelation.setSource(importMap.get(extendedType.getNameAsString()));
                } else {
                    pUmlRelation.setSource(pUmlClass.getPackageName() + "." + extendedType.getNameAsString());
                }
                pUmlRelation.setRelation("<|--");
                pUmlView.addPUmlRelation(pUmlRelation);

            }
        }

        super.visit(cORid, pUmlView);
    }

    private NodeList<ImportDeclaration> parseImport(Node node, PUmlClass pUmlClass, PUmlView pUmlView) {
        if (node instanceof CompilationUnit) {
            return ((CompilationUnit) node).getImports();
        } else if (node instanceof ClassOrInterfaceDeclaration) {
            pUmlClass.setClassName(((ClassOrInterfaceDeclaration) node).getNameAsString() + "." + pUmlClass.getClassName());

            Node parentNode = node.getParentNode().get();
            if (parentNode instanceof CompilationUnit) {
                PUmlRelation pUmlRelation = new PUmlRelation();
                pUmlRelation.setTarget(pUmlClass.getPackageName() + "." + pUmlClass.getClassName());
                pUmlRelation.setSource(pUmlClass.getPackageName() + "." + pUmlClass.getClassName().substring(0, pUmlClass.getClassName().lastIndexOf(".")));
                pUmlRelation.setRelation("+..");
                pUmlView.addPUmlRelation(pUmlRelation);
            }
            parseImport(parentNode, pUmlClass, pUmlView);
        }
        return null;
    }
}
