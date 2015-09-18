/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.qa.jcontainer.tomcat;

import org.apache.commons.lang3.StringUtils;

import org.jboss.qa.jcontainer.Container;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TomcatContainer<T extends TomcatConfiguration, U extends TomcatClient<T>, V extends TomcatUser>
		extends Container<T, U, V> {

	public TomcatContainer(T configuration) {
		super(configuration);
		configureServer();
	}

	protected void configureServer() {
		try {
			final File file = new File(configuration.getDirectory(), "conf" + File.separator + "server.xml");
			final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			final Document doc = docBuilder.parse(file);

			final XPathFactory xPathfactory = XPathFactory.newInstance();
			final XPath xpath = xPathfactory.newXPath();
			final XPathExpression expr = xpath.compile("/Server/Service/Connector[@protocol='HTTP/1.1']");

			final Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
			node.getAttributes().getNamedItem("port").setNodeValue(Integer.toString(configuration.getHttpPort()));

			// Write the content into xml file
			final TransformerFactory transformerFactory = TransformerFactory.newInstance();
			final Transformer transformer = transformerFactory.newTransformer();
			final DOMSource source = new DOMSource(doc);
			final StreamResult result = new StreamResult(file);
			transformer.transform(source, result);
		} catch (Exception e) {
			log.error("Ports was not configured", e);
		}
	}

	@Override
	public void addUser(V user) throws Exception {
		try {
			final File file = new File(configuration.getDirectory(), "conf" + File.separator + "tomcat-users.xml");
			final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			final Document doc = docBuilder.parse(file);

			// Get the root element
			final Node rootNode = doc.getElementsByTagName("tomcat-users").item(0);

			// Get existing roles
			final NodeList roleNodes = doc.getElementsByTagName("role");
			final List<String> existingRoles = new ArrayList<>();
			for (int i = 0; i < roleNodes.getLength(); i++) {
				existingRoles.add(roleNodes.item(i).getAttributes().getNamedItem("rolename").getNodeValue());
			}

			// Add new roles
			for (String role : user.getRoles()) {
				if (!existingRoles.contains(role)) {
					final Element roleEl = doc.createElement("role");
					roleEl.setAttribute("rolename", role);
					rootNode.appendChild(roleEl);
				}
			}

			// Get existing users
			final NodeList userNodes = doc.getElementsByTagName("user");
			final List<String> existingUsers = new ArrayList<>();
			for (int i = 0; i < userNodes.getLength(); i++) {
				existingUsers.add(userNodes.item(i).getAttributes().getNamedItem("username").getNodeValue());
			}

			final String newRoles = StringUtils.join(user.getRoles(), ",");
			if (!existingUsers.contains(user.getUsername())) { // Add new user
				final Element userEl = doc.createElement("user");
				userEl.setAttribute("username", user.getUsername());
				userEl.setAttribute("password", user.getPassword());
				userEl.setAttribute("roles", newRoles);
				rootNode.appendChild(userEl);
			} else { // Modify existing user
				for (int i = 0; i < userNodes.getLength(); i++) {
					if (user.getUsername().equals(userNodes.item(i).getAttributes().getNamedItem("username").getNodeValue())) {
						userNodes.item(i).getAttributes().getNamedItem("password").setNodeValue(user.getPassword());
						userNodes.item(i).getAttributes().getNamedItem("roles").setNodeValue(newRoles);
						log.warn("Existing user '{}' was modified", user.getUsername());
						break;
					}
				}
			}

			// Write the content into xml file
			final TransformerFactory transformerFactory = TransformerFactory.newInstance();
			final Transformer transformer = transformerFactory.newTransformer();
			final DOMSource source = new DOMSource(doc);
			final StreamResult result = new StreamResult(file);
			transformer.transform(source, result);
		} catch (Exception e) {
			log.error("User was not created", e);
		}
	}

	@Override
	public synchronized void stop() throws Exception {
		final ProcessBuilder processBuilder = new ProcessBuilder(configuration.generateStopCommand());
		final Process p = processBuilder.start();
		p.waitFor();
		log.info("Container was stopped");
	}

	@Override
	protected String getBasicCommand() {
		return null;
	}

	@Override
	protected String getLogDirInternal() throws Exception {
		final String logDir = configuration.getDirectory() + File.separator + "logs";
		final File logDirFile = new File(logDir);
		if (!logDirFile.exists() && !logDirFile.mkdirs()) {
			throw new IllegalStateException(String.format("Directory %s could not be created", logDir));
		}
		return logDir + File.separator;
	}
}
