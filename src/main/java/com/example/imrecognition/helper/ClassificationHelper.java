package com.example.imrecognition.helper;

import com.example.imrecognition.TextRecognitionManagement;
import com.meylemueller.common.util.exception.FinderException;
import com.meylemueller.pim2.objects.Classification;
import com.meylemueller.pim2.objects.Node;
import com.meylemueller.pim2.objects.Product;
import com.meylemueller.pim2.service.ClassificationFacade;
import com.meylemueller.pim2.service.NodeFacade;
import com.meylemueller.pim2.service.ProductFacade;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import com.example.imrecognition.communicators.IBMTagsCommunicator;

import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_KEY;
import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_URL;

/**
 * Gets tags and product id and links product to this categories
 */
public class ClassificationHelper
{
	private static final Logger logger = LogManager.getLogger(ClassificationHelper.class);
	private Map<String, Node> nodeEntityMap = new HashMap<>();
	private Map<String, Node> nodeElementMap = new HashMap<>();
	private Map<String, Node> nodeFullPathElementMap = new HashMap<>();
	public static final String LANGUAGE = "LANGUAGE";
	private static final String DEFAULT = "default";
	@Autowired
	private ClassificationFacade classificationFacade;
	@Autowired
	private NodeFacade nodeFacade;
	@Autowired
	private ProductFacade productFacade;
	@Autowired
	private IBMTagsCommunicator ibmTagsCommunicator;
	private boolean isLinked = false;

	/**
	 * Process raw info with general config attributes
	 * @param productId
	 * @param rawString
	 * @param attributes
	 * @throws FinderException
	 */
	public void linkProduct(Long productId, String rawString, Map<String, String> attributes) throws FinderException
	{
		String parentAttribute = attributes.get("CLASSIFICATION_PARENT");
		String targetAttribute = attributes.get("CLASSIFICATION_TARGET");
		String rawAttribute = attributes.get("CLASSIFICATION_RAW_INFO");
		String description = null;
		String wog = null;
		String wug = null;
		JsonElement attributeValues = new JsonParser().parse(rawString).getAsJsonObject().get("attributeValues");
		if (attributeValues != null)
		{
			JsonArray valuesAsJsonArray = attributeValues.getAsJsonArray();
			for (JsonElement value : valuesAsJsonArray)
			{
				if (value.getAsJsonObject().get(rawAttribute) != null)
				{
					description = value.getAsJsonObject().get(rawAttribute).getAsJsonObject().get(DEFAULT).getAsString();
					continue;
				}
				if (value.getAsJsonObject().get(parentAttribute) != null)
				{
					wog = value.getAsJsonObject().get(parentAttribute).getAsJsonObject().get(DEFAULT).getAsString();
					continue;
				}
				if (value.getAsJsonObject().get(targetAttribute) != null)
				{
					wug = value.getAsJsonObject().get(targetAttribute).getAsJsonObject().get(DEFAULT).getAsString();
					continue;
				}
			}
		}
		linkProductByInfo(productId, wog, wug, description, attributes);
	}

	/**
	 * @param productId
	 * @param wogKey
	 *            parent
	 * @param wugKey
	 *            current
	 */
	public void linkProductByInfo(Long productId, String wogKey, String wugKey, String description, Map<String, String> attributes)
	{
		isLinked = false;
		String rootAttribute = attributes.get("CLASSIFICATION_ROOT_LEVEL");
		String notFoundNodeLevel = attributes.get("CLASSIFICATION_NOT_FOUND_NODE");
		Classification classification = classificationFacade.findClassificationByName(rootAttribute);
		List<Node> nodes = nodeFacade.findAllNodesBelongingClassification(classification.getIdentity());
		if (nodeEntityMap.isEmpty())
		{
			nodes.forEach(node -> {
				nodeEntityMap.put(buildClassificationName(node).toLowerCase(), node);
				nodeElementMap.put(node.getIdentifier().toLowerCase(), node);
				nodeFullPathElementMap.put(buildFullNodesPath(node).toLowerCase(), node);
				logger.info(node.getIdentifier().toLowerCase());
			});
		}
		if (wogKey != null && wugKey != null)
		{
			processWogWugKeys(productId, wogKey, wugKey);
		}
		if (wogKey == null && wugKey != null && !isLinked)
		{
			processWugKey(productId, wugKey);
		}
		if (description != null && !isLinked)
		{
			processClassificationEngine(productId, description, classification, attributes);
		}
		if (!isLinked)
		{
			processWugKey(productId, notFoundNodeLevel);
		}
	}

	private void processClassificationEngine(Long productId, String description, Classification classification,
		Map<String, String> attributes)
	{
		processMatchingLogicIbm(productId, description, classification, attributes);
	}

	private void processMatchingLogicIbm(Long productId, String description, Classification classification,
		Map<String, String> attributes)
	{
		String ibmKey = attributes.get(TextRecognitionManagement.IBMTAGS_PROVIDER + CLIENT_API_KEY + LANGUAGE);
		String ibmUrl = attributes.get(TextRecognitionManagement.IBMTAGS_PROVIDER + CLIENT_API_URL + LANGUAGE);
		String ibmModelId = attributes.get(TextRecognitionManagement.IBMTAGS_PROVIDER + "MODEL_ID");
		Double thresholdValue = Double.valueOf(attributes.get("CLASSIFICATION_THRESHOLD_VALUE"));
		Map<String, String> configMaps = new HashMap<>();
		configMaps.put(TextRecognitionManagement.IBMTAGS_PROVIDER + CLIENT_API_KEY + LANGUAGE, ibmKey);
		configMaps.put(TextRecognitionManagement.IBMTAGS_PROVIDER + CLIENT_API_URL + LANGUAGE, ibmUrl);
		configMaps.put(TextRecognitionManagement.IBMTAGS_PROVIDER + "MODEL_ID", ibmModelId);
		String classificationResult = ibmTagsCommunicator.recognizeTags(description, configMaps).getClassification();
		if (classification != null)
		{
			String[] pathArray = classificationResult.split("\n");
			if (pathArray.length > 0)
			{
				String preferedPath = pathArray[0];
				String[] nodeScoreArray = preferedPath.split(":");
				if (nodeScoreArray.length > 1)
				{
					String node = nodeScoreArray[0];
					Double score = Double.valueOf(nodeScoreArray[1]);
					if (score >= thresholdValue)
					{
						logger.info("Trying link product to this higher node key: " + node + " and  score= " + score);
						processWugKey(productId, node);
					}
					else
					{
						logger.error("Score is: " + score + " for node: " + node);
					}
				}
			}
		}
		else
		{
			logger.error("Can't find selected node: " + classificationResult + " Check node name and list categories ");
		}
	}

	private void processWugKey(Long productId, String wugKey)
	{
		Node node;
		if (nodeElementMap.get(wugKey.toLowerCase()) != null)
		{
			node = nodeElementMap.get(wugKey.toLowerCase());
		}
		else
		{
			node = nodeFullPathElementMap.get(wugKey.toLowerCase());
		}
		if (node != null)
		{
			logger.info("Node was found. Link Product to node: " + wugKey);
			try
			{
				Product product = productFacade.findProductById(productId);
				product.getNodes().add(node);
				productFacade.save(Collections.singletonList(product));
				isLinked = true;
			}
			catch (FinderException e)
			{
				logger.error("Error linking node or product", e);
			}
		}
		else
		{
			logger.error("Error finding node or product: " + wugKey);
		}
	}

	/**
	 * Case when we know keys
	 * @param productId
	 * @param wogKey
	 * @param wugKey
	 */
	private void processWogWugKeys(Long productId, String wogKey, String wugKey)
	{
		String findName = wogKey + "/" + wugKey;
		if (nodeEntityMap.containsKey(findName.toLowerCase()))
		{
			logger.info("Node was found. Link Product to node: " + findName);
			try
			{
				Product product = productFacade.findProductById(productId);
				Node node = nodeEntityMap.get(findName.toLowerCase());
				product.getNodes().add(node);
				productFacade.save(Collections.singletonList(product));
				isLinked = true;
			}
			catch (FinderException e)
			{
				logger.error("Error linking product to node", e);
			}
		}
		else
		{
			logger.error("Node with this name was not found:" + findName);
		}
	}

	private String buildClassificationName(Node node)
	{
		String pathName = "";
		if (node.getParent() != null)
		{
			pathName = node.getParent().getIdentifier() + "/" + node.getIdentifier();
		}
		return pathName;
	}

	private String buildFullNodesPath(Node node)
	{
		String fullPath = node.getIdentifier() + "/";
		Node p1 = node.getParent();
		if (p1 != null)
		{
			fullPath = p1.getIdentifier() + "/" + fullPath;
			Node p2 = p1.getParent();
			if (p2 != null)
			{
				fullPath = p2.getIdentifier() + "/" + fullPath;
				Node p3 = p2.getParent();
				if (p3 != null)
				{
					fullPath = p3.getIdentifier() + "/" + fullPath;
					Node p4 = p3.getParent();
					if (p4 != null)
					{
						fullPath = p4.getIdentifier() + "/" + fullPath;
						Node p5 = p4.getParent();
						if (p5 != null)
						{
							fullPath = p5.getIdentifier() + "/" + fullPath;
							Node p6 = p5.getParent();
							if (p6 != null)
							{
								fullPath = p6.getIdentifier() + "/" + fullPath;
							}
						}
					}
				}
			}
		}
		return fullPath;
	}
}
