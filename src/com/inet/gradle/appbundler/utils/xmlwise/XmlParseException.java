package com.inet.gradle.appbundler.utils.xmlwise;

/**
 * Generic exception when parsing xml.
 * 
 * @author Christoffer Lerno
 */
public class XmlParseException extends Exception
{
    private static final long serialVersionUID = -3246260520113823143L;

    /**
     * Creates a new XmlParseException with a cause.
     * @param cause the cause
     */
    public XmlParseException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Creates a new XmlParseException with a message.
     * @param message the message
     */
    public XmlParseException(String message)
    {
        super(message);
    }

    /**
     * Creates a new XmlParseException with a message and cause.
     * @param message the message
     * @param cause the cause
     */
    public XmlParseException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
