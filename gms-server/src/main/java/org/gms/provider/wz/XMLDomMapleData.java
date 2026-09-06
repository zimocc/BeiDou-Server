/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gms.provider.wz;

import org.gms.constants.game.GameConstants;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.gms.provider.Data;
import org.gms.provider.DataEntity;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class XMLDomMapleData implements Data {
    private final Node node;
    private Path imageDataDir;

    public XMLDomMapleData(FileInputStream fis, Path imageDataDir) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(fis);
            this.node = document.getFirstChild();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.imageDataDir = imageDataDir;
    }

    private XMLDomMapleData(Node node) {
        this.node = node;
    }

    private Object getLock() {
        Document doc = node.getOwnerDocument();
        return doc != null ? doc : node;
    }

    @Override
    public Data getChildByPath(String path) {
        synchronized (getLock()) {
            String[] segments = path.split("/");
            if (segments.length > 0 && segments[0].equals("..")) {
                Data parent = (Data) getParent();
                if (parent == null) {
                    return null;
                }
                int slashIdx = path.indexOf("/");
                return slashIdx != -1 ? parent.getChildByPath(path.substring(slashIdx + 1)) : null;
            }

            Node myNode = node;
            for (String s : segments) {
                NodeList childNodes = myNode.getChildNodes();
                if (childNodes == null) {
                    return null;
                }
                boolean foundChild = false;
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node childNode = childNodes.item(i);
                    if (childNode != null && childNode.getNodeType() == Node.ELEMENT_NODE) {
                        NamedNodeMap attrs = childNode.getAttributes();
                        if (attrs != null) {
                            Node nameAttr = attrs.getNamedItem("name");
                            if (nameAttr != null && s.equals(nameAttr.getNodeValue())) {
                                myNode = childNode;
                                foundChild = true;
                                break;
                            }
                        }
                    }
                }
                if (!foundChild) {
                    return null;
                }
            }

            XMLDomMapleData ret = new XMLDomMapleData(myNode);
            if (imageDataDir != null) {
                String nodeName = getName();
                ret.imageDataDir = imageDataDir.resolve(nodeName != null ? nodeName.trim() : "").resolve(path).getParent();
            }
            return ret;
        }
    }

    @Override
    public List<Data> getChildren() {
        synchronized (getLock()) {
            List<Data> ret = new ArrayList<>();

            NodeList childNodes = node.getChildNodes();
            if (childNodes == null) {
                return ret;
            }
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                if (childNode != null && childNode.getNodeType() == Node.ELEMENT_NODE) {
                    XMLDomMapleData child = new XMLDomMapleData(childNode);
                    if (imageDataDir != null) {
                        String nodeName = getName();
                        child.imageDataDir = imageDataDir.resolve(nodeName != null ? nodeName.trim() : "");
                    }
                    ret.add(child);
                }
            }

            return ret;
        }
    }

    @Override
    public Object getData() {
        synchronized (getLock()) {
            NamedNodeMap attributes = node.getAttributes();
            if (attributes == null) {
                return null;
            }
            DataType type = getType();
            if (type == null) {
                return null;
            }
            switch (type) {
                case DOUBLE:
                case FLOAT:
                case INT:
                case SHORT: {
                    Node valNode = attributes.getNamedItem("value");
                    if (valNode == null) {
                        return null;
                    }
                    String value = valNode.getNodeValue();
                    Number nval = GameConstants.parseNumber(value);
                    if (nval == null) {
                        return null;
                    }

                    switch (type) {
                        case DOUBLE:
                            return nval.doubleValue();
                        case FLOAT:
                            return nval.floatValue();
                        case INT:
                            return nval.intValue();
                        case SHORT:
                            return nval.shortValue();
                        default:
                            return null;
                    }
                }
                case STRING:
                case UOL: {
                    Node valNode = attributes.getNamedItem("value");
                    return valNode != null ? valNode.getNodeValue() : null;
                }
                case VECTOR: {
                    Node xNode = attributes.getNamedItem("x");
                    Node yNode = attributes.getNamedItem("y");
                    if (xNode == null || yNode == null) {
                        return null;
                    }
                    return new Point(Integer.parseInt(xNode.getNodeValue()), Integer.parseInt(yNode.getNodeValue()));
                }
                case CANVAS: {
                    Node wNode = attributes.getNamedItem("width");
                    Node hNode = attributes.getNamedItem("height");
                    if (wNode == null || hNode == null) {
                        return null;
                    }
                    return new Point(Integer.parseInt(wNode.getNodeValue()), Integer.parseInt(hNode.getNodeValue()));
                }
                default:
                    return null;
            }
        }
    }

    @Override
    public DataType getType() {
        synchronized (getLock()) {
            String nodeName = node.getNodeName();
            if (nodeName == null) {
                return null;
            }
            switch (nodeName) {
                case "imgdir":
                    return DataType.PROPERTY;
                case "canvas":
                    return DataType.CANVAS;
                case "convex":
                    return DataType.CONVEX;
                case "sound":
                    return DataType.SOUND;
                case "uol":
                    return DataType.UOL;
                case "double":
                    return DataType.DOUBLE;
                case "float":
                    return DataType.FLOAT;
                case "int":
                    return DataType.INT;
                case "short":
                    return DataType.SHORT;
                case "string":
                    return DataType.STRING;
                case "vector":
                    return DataType.VECTOR;
                case "null":
                    return DataType.IMG_0x00;
            }
            return null;
        }
    }

    @Override
    public DataEntity getParent() {
        synchronized (getLock()) {
            Node parentNode = node.getParentNode();
            if (parentNode == null || parentNode.getNodeType() == Node.DOCUMENT_NODE) {
                return null;
            }
            XMLDomMapleData parentData = new XMLDomMapleData(parentNode);
            if (imageDataDir != null) {
                parentData.imageDataDir = imageDataDir.getParent();
            }
            return parentData;
        }
    }

    @Override
    public String getName() {
        synchronized (getLock()) {
            NamedNodeMap attrs = node.getAttributes();
            if (attrs != null) {
                Node nameAttr = attrs.getNamedItem("name");
                if (nameAttr != null) {
                    return nameAttr.getNodeValue();
                }
            }
            return "";
        }
    }

    @Override
    public Iterator<Data> iterator() {
        return getChildren().iterator();
    }

    /**
     * 获取指定节点属性值
     * @return
     */
    public String getAttributeValue(String name) {
        synchronized (getLock()) {
            NamedNodeMap attrs = node.getAttributes();
            if (attrs == null) {
                return null;
            }
            Node attr = attrs.getNamedItem(name);
            return attr == null ? null : attr.getNodeValue();
        }
    }
}
