import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.printer.PrettyPrinterConfiguration;

import java.util.Map;
import java.util.HashMap;

public class ASTConverter {

    private static final String isPackage = "isPackage";
    private static final String isImport = "isImport";
    private static final String isClassOrInterface = "isClassOrIsInterface";
    private static final String isMethod = "isMethod";
    private static final String isBlockComment = "isBlockComment";
    private static final String isComment = "isComment";
    private static final String isJavaDocComment = "isJavaDocComment";
    private static final String isNameExpr = "isNameExpr";
    private static final String isFieldAccessExpr = "isFieldAccessExpr";
    private static final String isConstruct = "isConstructor";
    private static final String isParameter = "isParameter";
    private static final String isLiteralString = "isStringConstant";
    private static final String isLiteralInteger = "isIntegerConstant";
    private static final String isLiteralDouble = "isDoubleConstant";
    private static final Boolean isLiteralBoolean = true;
    private static final String isVariable = "isVariable";

    private static Integer methodCounter = 1;

    private static Integer varCounter = 1;

    private static Map<String,String> mapMethod = null;
    private static Map<String,String> mapVar = null;


    public static String converter (String javaSourceCode, Integer version) {

        CompilationUnit compilationUnit = JavaParser.parse(javaSourceCode);

        if (version == 1) {

            version1(compilationUnit);


        } else if (version == 2) {
            // chama a segunda versao do AST

            version2(compilationUnit);

        } else {
            return "Please verify the version. It should be either 1 or 2";
        }

        String formattedJavaCode = prettyPrinter(compilationUnit);

        return formattedJavaCode;
    }


    private static void version1 (CompilationUnit compilationUnit) {
        transformPackageToIsPackage(compilationUnit);
        transformImportsToIsImports(compilationUnit);
        transformJavaOrInterfaceToIsClassOrInterface(compilationUnit);

        transformMethodDeclarationToIsMethod(compilationUnit);
        transformMethodCallExprToIsMethod(compilationUnit);

        transformVariableDeclarationToIsVariable(compilationUnit);
        transformParameterToIsParameter(compilationUnit);
        transformFieldAccessExprToIsFieldAccess(compilationUnit);
        transformNameExprToIsNameExpr(compilationUnit);

        transformBlockCommentToIsBlockComment(compilationUnit);
        transformCommentToIsComment(compilationUnit);
        transformJavaDocCommentToIsCJavaDocComment(compilationUnit);
        transformLineCommentToIsLineComment(compilationUnit);

        transformConstructorDeclarationToIsConstructor(compilationUnit);

        transformLiteralStringValueExprToIsConstant(compilationUnit);
        transformIntegerLiteralToIsConstant(compilationUnit);
        transformDoubleLiteralToIsConstant(compilationUnit);
        transformBooleanLiteralToIsConstant(compilationUnit);
        transformVariableDeclarationToIsVariable(compilationUnit);
        transformFieldAccessExprToIsFieldAccess(compilationUnit);


        transformCommentsNearPackagetToIsComment(compilationUnit);

        transformClassOrInterfaceTypeToIsClassOrInterfaceType(compilationUnit);

    }

    private static void version2 (CompilationUnit compilationUnit) {
        ASTConverter.mapMethod = new HashMap<String,String>();
        ASTConverter.mapVar = new HashMap<String,String>();
        mapMethod.clear();
        mapVar.clear();

        ASTConverter.methodCounter = 1;
        ASTConverter.varCounter = 1;

        transformPackageToIsPackage(compilationUnit);
        transformImportsToIsImports(compilationUnit);
        transformJavaOrInterfaceToIsClassOrInterface(compilationUnit);

        transformMethodDeclarationToIsMethod2(compilationUnit);
        transformMethodCallExprToIsMethod2(compilationUnit);
        transformCallableDeclarationToIsMethod(compilationUnit);
        transformVariableDeclarationToIsVariable2(compilationUnit);
        transformParameterToIsParameter2(compilationUnit);
        transformFieldAccessExprToIsFieldAccess2(compilationUnit);
        transformNameExprToIsNameExpr2(compilationUnit);


        transformBlockCommentToIsBlockComment(compilationUnit);
        transformCommentToIsComment(compilationUnit);
        transformJavaDocCommentToIsCJavaDocComment(compilationUnit);
        transformLineCommentToIsLineComment(compilationUnit);

        transformConstructorDeclarationToIsConstructor(compilationUnit);

        transformLiteralStringValueExprToIsConstant(compilationUnit);
        transformIntegerLiteralToIsConstant(compilationUnit);
        transformDoubleLiteralToIsConstant(compilationUnit);
        transformBooleanLiteralToIsConstant(compilationUnit);

        transformCommentsNearPackagetToIsComment(compilationUnit);
        transformCommentsNearPackagetToIsComment2(compilationUnit);
        transformClassOrInterfaceTypeToIsClassOrInterfaceType(compilationUnit);

    }

    private static void transformPackageToIsPackage(CompilationUnit cUnit) {
        cUnit.findAll(PackageDeclaration.class).stream().
                forEach(p -> {
                    p.setName(ASTConverter.isPackage);
                });
    }

    private static void transformImportsToIsImports(CompilationUnit cUnit) {
        cUnit.findAll(ImportDeclaration.class).stream().
                forEach(p -> {
                    p.setName(ASTConverter.isImport);
                });
    }

    private static void transformJavaOrInterfaceToIsClassOrInterface(CompilationUnit cUnit) {
        cUnit.findAll(ClassOrInterfaceDeclaration.class).stream()
                .forEach(c -> {
//                    c.getAllContainedComments().forEach(comment ->  {
//                        comment.setContent(TransformJavaFiles.isComment);
//                    });
                    if (c.getExtendedTypes().size() > 0) {
                        c.getExtendedTypes().forEach(exT -> {
                            exT.setName("isExtend");
                        });
                    }
                    if (c.getImplementedTypes().size() > 0) {
                        c.getImplementedTypes().forEach(imT -> {
                            imT.setName("isImplement");
                        });
                    }
                    c.setName(ASTConverter.isClassOrInterface);
                });
    }


    private static void transformMethodDeclarationToIsMethod2(CompilationUnit cUnit) {


        cUnit.findAll(MethodDeclaration.class).forEach(mce -> {

            String methodName = mce.getName().asString();
            Integer methodNumber = ASTConverter.methodCounter++;

            if (!ASTConverter.mapMethod.containsKey(methodName)) {

                ASTConverter.mapMethod.put(methodName, "method"+methodNumber);
            }else {
                methodNumber--;
                ASTConverter.methodCounter--;
            }


            mce.setName("method"+methodNumber);


        });

    }

    private static void transformMethodDeclarationToIsMethod(CompilationUnit cUnit) {
        cUnit.findAll(MethodDeclaration.class).stream()
                .forEach(c -> {
//                    c.getAllContainedComments().forEach(comment ->  {
//                        comment.setContent(TransformJavaFiles.isComment);
//                    });
                    c.setName("method"+ASTConverter.methodCounter++);
                    c.setName(ASTConverter.isMethod);
                });
    }

    private static void transformBlockCommentToIsBlockComment(CompilationUnit cUnit) {


        cUnit.findAll(BlockComment.class).stream()
                .forEach(c -> {
//                    c.getAllContainedComments().forEach(comment ->  {
//                        comment.setContent(TransformJavaFiles.isBlockComment);
//                    });
//                    c.setBlockComment(com.br.TransformJavaFiles.isBlockComment);
                });
    }

    private static void transformCommentToIsComment(CompilationUnit cUnit) {
        cUnit.findAll(Comment.class).stream()
                .forEach(c -> {
//                    c.setContent(com.br.TransformJavaFiles.isComment); testar aqui pois deu erro
                });
    }

    private static void transformJavaDocCommentToIsCJavaDocComment(CompilationUnit cUnit) {
        cUnit.findAll(JavadocComment.class).stream()
                .forEach(c -> {
//                    c.setComment(c.setContent(com.br.TransformJavaFiles.isComment));
                });
    }

    private static void transformLineCommentToIsLineComment(CompilationUnit cUnit) {
        cUnit.findAll(LineComment.class).stream()
                .forEach(c -> {
//                    c.setComment(c.setContent(com.br.TransformJavaFiles.isComment));
                });
    }

    private static void transformCallableDeclarationToIsMethod(CompilationUnit cUnit) {
        cUnit.findAll(CallableDeclaration.class).stream()
                .forEach(c -> {
//                    c.getAllContainedComments().forEach(comment ->  {
//                        comment.setContent(TransformJavaFiles.isComment);
//                    });


                    String methodCallName = c.getName().toString();

                    if (ASTConverter.mapMethod.containsKey(methodCallName)) {
                        c.setName(ASTConverter.mapMethod.get(methodCallName));
                    }

                });

    }


    private static void transformMethodCallExprToIsMethod2(CompilationUnit cUnit) {
        cUnit.findAll(MethodCallExpr.class).forEach(mce -> {


            String methodCallName = mce.getName().toString();

            if (mapMethod.containsKey(methodCallName)) {
                mce.setName(ASTConverter.mapMethod.get(methodCallName));
            }else {
                Integer methodNumber = ASTConverter.methodCounter++;
                mapMethod.put(methodCallName, "method"+methodNumber);
                mce.setName("method"+methodNumber);
            }


        });
    }

    private static void transformMethodCallExprToIsMethod(CompilationUnit cUnit) {
        cUnit.findAll(MethodCallExpr.class).stream()
                .forEach(c -> {
//                    c.getAllContainedComments().forEach(comment ->  {
//                        comment.setContent(TransformJavaFiles.isComment);
//                    });
                    c.setName(ASTConverter.isMethod);
                });
    }

    private static void transformNameExprToIsNameExpr(CompilationUnit cUnit) {
        cUnit.findAll(NameExpr.class).stream()
                .forEach(c -> {
//                    c.getAllContainedComments().forEach(comment ->  {
//                        comment.setContent(TransformJavaFiles.isComment);
//                    });
                    c.setName(ASTConverter.isNameExpr);
                });
    }


    private static void transformNameExprToIsNameExpr2(CompilationUnit cUnit) {
        cUnit.findAll(NameExpr.class).forEach(c -> {
            String fiedName = c.getName().toString();

            if (mapVar.containsKey(fiedName)) {
                c.setName(mapVar.get(fiedName));
            }else {
                Integer VarNumber = ASTConverter.varCounter++;
                mapVar.put(fiedName, "var"+VarNumber);
                c.setName("var"+VarNumber);
            }
        });
    }

    private static void transformFieldAccessExprToIsFieldAccess(CompilationUnit cUnit) {
        cUnit.findAll(FieldAccessExpr.class).stream()
                .forEach(c -> {
//                    c.getAllContainedComments().forEach(comment ->  {
//                        comment.setContent(TransformJavaFiles.isComment);
//                    });
                    c.setName(ASTConverter.isFieldAccessExpr);
                });
    }

    private static void transformFieldAccessExprToIsFieldAccess2(CompilationUnit cUnit) {
        cUnit.findAll(FieldAccessExpr.class).forEach(c -> {


            String fiedName = c.getName().toString();

            if (mapVar.containsKey(fiedName)) {
                c.setName(mapVar.get(fiedName));
            } else {
                Integer VarNumber = ASTConverter.varCounter++;
                mapVar.put(fiedName, "var" + VarNumber);
                c.setName("var" + VarNumber);
            }


        });
    }

    private static void transformConstructorDeclarationToIsConstructor(CompilationUnit cUnit) {
        cUnit.findAll(ConstructorDeclaration.class).stream()
                .forEach(c -> {
//                    c.getAllContainedComments().forEach(comment ->  {
//                        comment.setContent(TransformJavaFiles.isComment);
//                    });
                    c.setName(ASTConverter.isConstruct);
                });
    }

    private static void transformParameterToIsParameter(CompilationUnit cUnit) {
        cUnit.findAll(Parameter.class).stream()
                .forEach(c -> {
//                    c.getAllContainedComments().forEach(comment ->  {
//                        comment.setContent(TransformJavaFiles.isComment);
//                    });
                    c.setName(ASTConverter.isParameter);
                });
    }

    private static void transformParameterToIsParameter2(CompilationUnit cUnit) {
        cUnit.findAll(Parameter.class).forEach(c -> {
            String methodName = c.getName().asString();
            Integer varNumber = ASTConverter.varCounter++;

            if (!mapVar.containsKey(methodName)) {

                mapVar.put(methodName, "var"+varNumber);
            }else {
                varNumber--;
                ASTConverter.varCounter--;
            }


            c.setName("var"+varNumber);
        });
    }

    private static void transformLiteralStringValueExprToIsConstant(CompilationUnit cUnit) {
        cUnit.findAll(LiteralStringValueExpr.class).stream()
                .forEach(c -> {
//                    c.getAllContainedComments().forEach(comment ->  {
//                        comment.setContent(TransformJavaFiles.isComment);
//                    });
                    c.setValue(ASTConverter.isLiteralString);
                });
    }

    private static void transformIntegerLiteralToIsConstant(CompilationUnit cUnit) {
        cUnit.findAll(IntegerLiteralExpr.class).stream()
                .forEach(c -> {
//                    c.getAllContainedComments().forEach(comment ->  {
//                        comment.setContent(TransformJavaFiles.isComment);
//                    });
                    c.setValue(ASTConverter.isLiteralInteger);
                });
    }

    private static void transformDoubleLiteralToIsConstant(CompilationUnit cUnit) {
        cUnit.findAll(DoubleLiteralExpr.class).stream()
                .forEach(c -> {
//                    c.getAllContainedComments().forEach(comment ->  {
//                        comment.setContent(TransformJavaFiles.isComment);
//                    });
                    c.setValue(ASTConverter.isLiteralDouble);
                });
    }

    private static void transformBooleanLiteralToIsConstant(CompilationUnit cUnit) {
        cUnit.findAll(BooleanLiteralExpr.class).stream()
                .forEach(c -> {
//                    c.getAllContainedComments().forEach(comment ->  {
//                        comment.setContent(TransformJavaFiles.isComment);
//                    });
                    c.setValue(ASTConverter.isLiteralBoolean);
                });
    }


    private static void transformVariableDeclarationToIsVariable(CompilationUnit cUnit) {
        cUnit.findAll(VariableDeclarator.class).stream()
                .forEach(c -> {
//                    c.getAllContainedComments().forEach(comment ->  {
//                        comment.setContent(TransformJavaFiles.isComment);
//                    });
                    c.setType("isType");
                    c.setName(ASTConverter.isVariable);
                });
    }

    private static void transformVariableDeclarationToIsVariable2(CompilationUnit cUnit) {
        cUnit.findAll(VariableDeclarator.class).forEach(c -> {

            String methodName = c.getName().asString();
            Integer varNumber = ASTConverter.varCounter++;

            if (!mapVar.containsKey(methodName)) {

                mapVar.put(methodName, "var"+varNumber);
            }else {
                varNumber--;
                ASTConverter.varCounter--;
            }


            c.setName("var"+varNumber);


        });
    }

    private static void transformClassOrInterfaceTypeToIsClassOrInterfaceType (CompilationUnit cUnit) {
        cUnit.findAll(ClassOrInterfaceType.class).stream()
                .forEach(c -> {
                    c.setName("isClassOrInterfaceInstantiation");
                });
    }



    private static void transformCommentsNearPackagetToIsComment(CompilationUnit cUnit) {
        cUnit.findAll(PackageDeclaration.class).stream()
                .forEach(c -> {
                    if (c.getParentNode().get() instanceof CompilationUnit) {
                        CompilationUnit cu = (CompilationUnit) c.getParentNode().get();

//                    cu.getComments().forEach(comment -> comment.setContent(TransformJavaFiles.isComment));

                    }

                });

    }

    private static void transformCommentsNearPackagetToIsComment2(CompilationUnit cUnit) {
        cUnit.findAll(PackageDeclaration.class).stream()
                .forEach(c -> {
                    if (c.getParentNode().get() instanceof CompilationUnit) {
                        CompilationUnit cu = (CompilationUnit) c.getParentNode().get();

//                        cu.setComment(new LineComment(TransformJavaFiles.isComment));

                    }

                });

    }

    private static String prettyPrinter(CompilationUnit cUnit) {

        PrettyPrinterConfiguration conf = new PrettyPrinterConfiguration();
        conf.setIndentType(PrettyPrinterConfiguration.IndentType.SPACES);
        conf.setPrintComments(false);


        String newJavaClass = cUnit.toString(conf);

        return newJavaClass;

    }


}
