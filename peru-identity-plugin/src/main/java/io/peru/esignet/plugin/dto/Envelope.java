package io.peru.esignet.plugin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "Envelope", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Envelope {

    @JacksonXmlProperty(localName = "Body")
    private Body body;

    public Body getBody() { return body; }
    public void setBody(Body body) { this.body = body; }
}