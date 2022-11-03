package com.example.imrecognition;

import com.example.imrecognition.helper.TranslatorHelper;
import com.meylemueller.base.project.facade.ProjectTypeAttributeFacade;
import com.meylemueller.base.project.model.ProjectAttributeValueEntity;
import com.meylemueller.base.project.model.ProjectEntity;
import com.meylemueller.base.project.model.ProjectTypeAttributeEntity;
import com.meylemueller.base.project.service.ProjectAttributeValueService;
import com.meylemueller.projectmanagement.model.api.ClientProjectAttributeValue;
import com.meylemueller.projectmanagement.model.api.ClientProjectFacade;
import com.meylemueller.projectmanagement.model.api.ClientTableMetaData;
import com.meylemueller.projectmanagement.model.facade.ProjectFacadeImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.imrecognition.transformers.MapHelper;

public class OmnTranslationService
{
	private static final Logger logger = LogManager.getLogger(OmnTranslationService.class);
	private static final String TRANSLATION_KEY_DE = "KEY_DE1";
	private static final String TRANSLATION_KEY_EN = "KEY_EN1";
	private static final String TRANSLATION_STORAGE = "STORAGE1";
	private Map<String, String> enDeMap = new HashMap<>();
	@Autowired
	private ClientProjectFacade prjFacade;
	@Autowired
	private ProjectFacadeImpl projectFacade;
	@Autowired
	private ProjectTypeAttributeFacade projectTypeAttributeFacade;
	@Autowired
	private ProjectAttributeValueService projectAttributeValueService;

	public Map<String, String> translate(String subscriptionKey, String host, String path, String params, String textArray)
	{
		try
		{
			ProjectEntity configProject = projectFacade.findByIdentity(Long.valueOf(1350970));
			if (enDeMap.isEmpty())
			{
				ClientTableMetaData storageInfoRaw = prjFacade.getTableMetaData(configProject.getIdentity(), TRANSLATION_STORAGE);
				fillStorage(storageInfoRaw);
			}
			return translateText(subscriptionKey, host, path, params, textArray, configProject);
		}
		catch (Exception e)
		{
			logger.error("Error getting translation ", e);
		}
		return Collections.emptyMap();
	}

	private void fillStorage(ClientTableMetaData metaData)
	{
		for (long i = 0; i < metaData.size(); i++)
		{
			ClientProjectAttributeValue value = metaData.getValue(i, TRANSLATION_KEY_EN);
			if (value != null && value.getvString() != null)
			{
				String keyEn = value.getvString();
				String keyDe = metaData.getValue(i, TRANSLATION_KEY_DE).getvString();
				enDeMap.put(keyEn, keyDe);
			}
		}
	}

	private void updateConfigInfo(Map<String, String> newAttributes, ProjectEntity projectEntity)
	{
		logger.info("Update project with new values: " + newAttributes.toString());
		Integer newCount = newAttributes.size();
		Integer lastIndex = enDeMap.size() - newCount;
		Long projectTypeId = projectEntity.getProjectType().getIdentity();
		ProjectTypeAttributeEntity attributeTypeEntityEn =
			projectTypeAttributeFacade.findByProjectTypeIdentity(projectTypeId, TRANSLATION_KEY_EN);
		ProjectTypeAttributeEntity attributeTypeEntityDe =
			projectTypeAttributeFacade.findByProjectTypeIdentity(projectTypeId, TRANSLATION_KEY_DE);
		try
		{
			for (String enKey : newAttributes.keySet())
			{
				ProjectAttributeValueEntity projectAttributeValueEntityEn = new ProjectAttributeValueEntity();
				projectAttributeValueEntityEn.setLanguageIdentity(null);
				projectAttributeValueEntityEn.setTableOrder(lastIndex.longValue());
				projectAttributeValueEntityEn.setAttributeType(attributeTypeEntityEn);
				projectAttributeValueEntityEn.setProject(projectEntity);
				projectAttributeValueEntityEn.setVString(enKey);
				ProjectAttributeValueEntity projectAttributeValueEntityDe = new ProjectAttributeValueEntity();
				projectAttributeValueEntityDe.setLanguageIdentity(null);
				projectAttributeValueEntityDe.setTableOrder(lastIndex.longValue());
				projectAttributeValueEntityDe.setAttributeType(attributeTypeEntityDe);
				projectAttributeValueEntityDe.setProject(projectEntity);
				projectAttributeValueEntityDe.setVString(newAttributes.get(enKey));
				projectAttributeValueService.saveOrUpdate(projectAttributeValueEntityEn);
				projectAttributeValueService.saveOrUpdate(projectAttributeValueEntityDe);
				logger.info("Attribute was created " + enKey + ":" + newAttributes.get(enKey) + " - " + lastIndex);
				lastIndex++;
			}
		}
		catch (Exception e)
		{
			logger.error("Error saving attribute ", e);
		}
	}

	/**
	 * Text for translation
	 * @param subscriptionKey
	 * @param host
	 * @param path
	 * @param params
	 * @param rawText
	 * @param configProject
	 * @return
	 */
	private Map<String, String> translateText(String subscriptionKey, String host, String path, String params, String rawText,
		ProjectEntity configProject)
	{
		Map<String, Double> translatedText = new HashMap<>();
		Map<String, String> translatedStringText = new HashMap<>();
		Map<String, String> newAttributes = new HashMap<>();
		String[] keywordsForTranslation = rawText.split(";");
		for (int i = 0; i < keywordsForTranslation.length; i++)
		{
			String keyValue = keywordsForTranslation[i];
			String[] values = keyValue.split(":");
			if (values.length > 1)
			{
				String enKey = values[0].trim().toLowerCase();
				String scoreValue = values[1];
				if (isDouble(scoreValue))
				{
					if (enDeMap.containsKey(enKey))
					{
						translatedText.put(enDeMap.get(enKey), Double.valueOf(scoreValue));
					}
					else
					{
						String deKey = translateWord(subscriptionKey, host, path, params, enKey).toLowerCase();
						enDeMap.put(enKey, deKey);
						translatedText.put(deKey, Double.valueOf(scoreValue));
						newAttributes.put(enKey, deKey);
					}
				}
				else
				{
					String deKey;
					String deScore;
					if (!enDeMap.containsKey(enKey))
					{
						deKey = translateWord(subscriptionKey, host, path, params, enKey).toLowerCase();
						enDeMap.put(enKey, deKey);
						newAttributes.put(enKey, deKey);
					}
					if (!enDeMap.containsKey(scoreValue.toLowerCase()))
					{
						deScore = translateWord(subscriptionKey, host, path, params, scoreValue).toLowerCase();
						enDeMap.put(scoreValue.toLowerCase(), deScore);
						newAttributes.put(scoreValue.toLowerCase(), deScore);
					}
					translatedStringText.put(enDeMap.get(enKey), enDeMap.get(scoreValue));
				}
			}
		}
		if (!newAttributes.isEmpty())
		{
			updateConfigInfo(newAttributes, configProject);
		}
		Map<String, String> resultFromDoubleValues = MapHelper.sortMap(translatedText);
		resultFromDoubleValues.putAll(translatedStringText);
		return resultFromDoubleValues;
	}

	private boolean isDouble(String value)
	{
		try
		{
			Double.valueOf(value);
			return true;
		}
		catch (NumberFormatException e)
		{
			logger.error("Error convert value to double " + value);
			return false;
		}
	}

	private String translateWord(String subscriptionKey, String host, String path, String params, String textForTranslation)
	{
		return TranslatorHelper.translateWord(subscriptionKey, host, path, params, textForTranslation);
	}
}
