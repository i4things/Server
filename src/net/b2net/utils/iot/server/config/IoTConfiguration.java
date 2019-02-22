//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.2-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.02.22 at 12:53:16 PM GMT 
//


package net.b2net.utils.iot.server.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="Server">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="IP" type="{http://www.w3.org/2001/XMLSchema}string" default="0.0.0.0" />
 *                 &lt;attribute name="Port" type="{http://www.w3.org/2001/XMLSchema}int" default="5409" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="JsonServer">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="IP" type="{http://www.w3.org/2001/XMLSchema}string" default="0.0.0.0" />
 *                 &lt;attribute name="Port" type="{http://www.w3.org/2001/XMLSchema}int" default="5408" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="DatabaseProvider">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="Args" type="{http://www.w3.org/2001/XMLSchema}string" default="" />
 *                 &lt;attribute name="Instance" type="{http://www.w3.org/2001/XMLSchema}string" default="net.b2net.utils.iot.server.storage.DatabaseProviderPgSQL" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="StreamingProvider">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="Args" type="{http://www.w3.org/2001/XMLSchema}string" default="" />
 *                 &lt;attribute name="Instance" type="{http://www.w3.org/2001/XMLSchema}string" default="" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {

})
@XmlRootElement(name = "IoTConfiguration")
public class IoTConfiguration {

    @XmlElement(name = "Server", required = true)
    protected IoTConfiguration.Server server;
    @XmlElement(name = "JsonServer", required = true)
    protected IoTConfiguration.JsonServer jsonServer;
    @XmlElement(name = "DatabaseProvider", required = true)
    protected IoTConfiguration.DatabaseProvider databaseProvider;
    @XmlElement(name = "StreamingProvider", required = true)
    protected IoTConfiguration.StreamingProvider streamingProvider;

    /**
     * Gets the value of the server property.
     * 
     * @return
     *     possible object is
     *     {@link IoTConfiguration.Server }
     *     
     */
    public IoTConfiguration.Server getServer() {
        return server;
    }

    /**
     * Sets the value of the server property.
     * 
     * @param value
     *     allowed object is
     *     {@link IoTConfiguration.Server }
     *     
     */
    public void setServer(IoTConfiguration.Server value) {
        this.server = value;
    }

    /**
     * Gets the value of the jsonServer property.
     * 
     * @return
     *     possible object is
     *     {@link IoTConfiguration.JsonServer }
     *     
     */
    public IoTConfiguration.JsonServer getJsonServer() {
        return jsonServer;
    }

    /**
     * Sets the value of the jsonServer property.
     * 
     * @param value
     *     allowed object is
     *     {@link IoTConfiguration.JsonServer }
     *     
     */
    public void setJsonServer(IoTConfiguration.JsonServer value) {
        this.jsonServer = value;
    }

    /**
     * Gets the value of the databaseProvider property.
     * 
     * @return
     *     possible object is
     *     {@link IoTConfiguration.DatabaseProvider }
     *     
     */
    public IoTConfiguration.DatabaseProvider getDatabaseProvider() {
        return databaseProvider;
    }

    /**
     * Sets the value of the databaseProvider property.
     * 
     * @param value
     *     allowed object is
     *     {@link IoTConfiguration.DatabaseProvider }
     *     
     */
    public void setDatabaseProvider(IoTConfiguration.DatabaseProvider value) {
        this.databaseProvider = value;
    }

    /**
     * Gets the value of the streamingProvider property.
     * 
     * @return
     *     possible object is
     *     {@link IoTConfiguration.StreamingProvider }
     *     
     */
    public IoTConfiguration.StreamingProvider getStreamingProvider() {
        return streamingProvider;
    }

    /**
     * Sets the value of the streamingProvider property.
     * 
     * @param value
     *     allowed object is
     *     {@link IoTConfiguration.StreamingProvider }
     *     
     */
    public void setStreamingProvider(IoTConfiguration.StreamingProvider value) {
        this.streamingProvider = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="Args" type="{http://www.w3.org/2001/XMLSchema}string" default="" />
     *       &lt;attribute name="Instance" type="{http://www.w3.org/2001/XMLSchema}string" default="net.b2net.utils.iot.server.storage.DatabaseProviderPgSQL" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class DatabaseProvider {

        @XmlAttribute(name = "Args")
        protected String args;
        @XmlAttribute(name = "Instance")
        protected String instance;

        /**
         * Gets the value of the args property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getArgs() {
            if (args == null) {
                return "";
            } else {
                return args;
            }
        }

        /**
         * Sets the value of the args property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setArgs(String value) {
            this.args = value;
        }

        /**
         * Gets the value of the instance property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getInstance() {
            if (instance == null) {
                return "net.b2net.utils.iot.server.storage.DatabaseProviderPgSQL";
            } else {
                return instance;
            }
        }

        /**
         * Sets the value of the instance property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setInstance(String value) {
            this.instance = value;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="IP" type="{http://www.w3.org/2001/XMLSchema}string" default="0.0.0.0" />
     *       &lt;attribute name="Port" type="{http://www.w3.org/2001/XMLSchema}int" default="5408" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class JsonServer {

        @XmlAttribute(name = "IP")
        protected String ip;
        @XmlAttribute(name = "Port")
        protected Integer port;

        /**
         * Gets the value of the ip property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getIP() {
            if (ip == null) {
                return "0.0.0.0";
            } else {
                return ip;
            }
        }

        /**
         * Sets the value of the ip property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setIP(String value) {
            this.ip = value;
        }

        /**
         * Gets the value of the port property.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public int getPort() {
            if (port == null) {
                return  5408;
            } else {
                return port;
            }
        }

        /**
         * Sets the value of the port property.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setPort(Integer value) {
            this.port = value;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="IP" type="{http://www.w3.org/2001/XMLSchema}string" default="0.0.0.0" />
     *       &lt;attribute name="Port" type="{http://www.w3.org/2001/XMLSchema}int" default="5409" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Server {

        @XmlAttribute(name = "IP")
        protected String ip;
        @XmlAttribute(name = "Port")
        protected Integer port;

        /**
         * Gets the value of the ip property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getIP() {
            if (ip == null) {
                return "0.0.0.0";
            } else {
                return ip;
            }
        }

        /**
         * Sets the value of the ip property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setIP(String value) {
            this.ip = value;
        }

        /**
         * Gets the value of the port property.
         * 
         * @return
         *     possible object is
         *     {@link Integer }
         *     
         */
        public int getPort() {
            if (port == null) {
                return  5409;
            } else {
                return port;
            }
        }

        /**
         * Sets the value of the port property.
         * 
         * @param value
         *     allowed object is
         *     {@link Integer }
         *     
         */
        public void setPort(Integer value) {
            this.port = value;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="Args" type="{http://www.w3.org/2001/XMLSchema}string" default="" />
     *       &lt;attribute name="Instance" type="{http://www.w3.org/2001/XMLSchema}string" default="" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class StreamingProvider {

        @XmlAttribute(name = "Args")
        protected String args;
        @XmlAttribute(name = "Instance")
        protected String instance;

        /**
         * Gets the value of the args property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getArgs() {
            if (args == null) {
                return "";
            } else {
                return args;
            }
        }

        /**
         * Sets the value of the args property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setArgs(String value) {
            this.args = value;
        }

        /**
         * Gets the value of the instance property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getInstance() {
            if (instance == null) {
                return "";
            } else {
                return instance;
            }
        }

        /**
         * Sets the value of the instance property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setInstance(String value) {
            this.instance = value;
        }

    }

}
