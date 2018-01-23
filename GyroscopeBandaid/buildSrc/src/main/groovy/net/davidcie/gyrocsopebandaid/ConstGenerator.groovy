package net.davidcie.gyroscopebandaid

import com.sun.codemodel.ClassType
import com.sun.codemodel.JCodeModel
import com.sun.codemodel.JExpr

import static com.sun.codemodel.JMod.*
import static com.sun.codemodel.JExpr.*

public class ConstGenerator {

    /**
     * Generates <code>pl.droidsonroids.domainnameutils.TldList</code> and places it into <code>outputDir</code>.
     * @param outputDir directory where generated sources will be written to
     */
    public static void generateConstClass(final File outputDir, final File xmlSourceFile) {
        def xmlFile = new XmlSlurper().parse(xmlSourceFile)
        def javadocConfig = new ConfigSlurper()
                .parse(ConstGenerator.class.getResource('javadoc.properties'))
        def codeModel = new JCodeModel()

        // Create a class definition
        def definedClassName = ConstGenerator.class.getPackage().getName() + '.Const'
        def definedClass = codeModel._class(PUBLIC | FINAL, definedClassName, ClassType.CLASS)
        def definedClassJavadoc = definedClass.javadoc()
        definedClassJavadoc.append(javadocConfig.getProperty('classJavadoc'))

        // String resources
        xmlFile.children().each { node ->
            def fielfName = node.@name.text().toUpperCase(Locale.ENGLISH)
            def fieldValue
            def fieldClass
            if ( node.name() == 'string' ) {
                fieldValue = node.text()
                fieldClass = String.class
            } else if ( node.name() == 'integer' ) {
                fieldValue = node.text().toInteger()
                fieldClass = int.class
            } else if ( node.name() == 'item' ) {
                fieldValue = node.text().toFloat()
                fieldClass = float.class
            }

            definedClass.field(PUBLIC | STATIC | FINAL, fieldClass, fielfName, JExpr.lit(fieldValue))
        }


        if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
            throw new IOException('Could not create directory: ' + outputDir)
        }
        codeModel.build(outputDir)
    }
}
