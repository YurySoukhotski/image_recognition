package com.example.imrecognition.transformers;

import java.util.List;
import java.util.Map;

public class RecognitionModel
{
	private String rawData;
	private Map<String, String> tagsInfo;
	private Map<String, String> tagsInfoDe;
	private Map<String, String> categoryInfo;
	private Map<String, String> colorInfo;
	private Map<String, String> captionInfo;
	private Map<String, String> faceInfo;
	private Map<String, String> celebrityInfo;
	private Map<String, String> ocrInfo;
	private String faceRaw;
	private String statusInfo;
	private Map<String, String> landmarks;
	private Map<String, String> nswf;
	private Map<String, String> imageType;
	private String isBlackAndWhite;
	private String entities;
	private Map<String, String> logo;
	private Map<String, String> moderation;
	private Map<String, String> portraitQuality;
	private Map<String, String> landscape;
	private Map<String, String> food;
	// from text recognition
	private List<String> keyPhrases;
	private String language;
	private String sentiment;
	private List<String> linkedEntities;
	private String emotions;
	private String generatedText;
	private Map<String, Double> nodeAnaliseStatus;
	private Map<String, String> bikes;

	private Map<String, List<String>> objectAnnotations;
	private String abuse;
	private String intent;
	private String classification;
	private String classificationCustom;
	private List<String> concepts;
	private List<String> categories;
	private String node;

	public List<String> getConcepts()
	{
		return concepts;
	}

	public Map<String, String> getBikes() {
		return bikes;
	}

	public Map<String, List<String>> getObjectAnnotations() {
		return objectAnnotations;
	}

	public void setObjectAnnotations(Map<String, List<String>> objectAnnotations) {
		this.objectAnnotations = objectAnnotations;
	}

	public void setBikes(Map<String, String> bikes) {
		this.bikes = bikes;
	}

	public void setConcepts(List<String> concepts)
	{
		this.concepts = concepts;
	}

	public List<String> getCategories()
	{
		return categories;
	}

	public void setCategories(List<String> categories)
	{
		this.categories = categories;
	}

	public String getClassification()
	{
		return classification;
	}

	public void setClassification(String classification)
	{
		this.classification = classification;
	}

	public String getAbuse()
	{
		return abuse;
	}

	public void setAbuse(String abuse)
	{
		this.abuse = abuse;
	}

	public String getIntent()
	{
		return intent;
	}

	public void setIntent(String intent)
	{
		this.intent = intent;
	}

	public Map<String, Double> getNodeAnaliseStatus()
	{
		return nodeAnaliseStatus;
	}

	public void setNodeAnaliseStatus(Map<String, Double> nodeAnaliseStatus)
	{
		this.nodeAnaliseStatus = nodeAnaliseStatus;
	}

	public String getGeneratedText()
	{
		return generatedText;
	}

	public void setGeneratedText(String generatedText)
	{
		this.generatedText = generatedText;
	}

	public Map<String, String> getFood()
	{
		return food;
	}

	public void setFood(Map<String, String> food)
	{
		this.food = food;
	}

	public Map<String, String> getLandscape()
	{
		return landscape;
	}

	public void setLandscape(Map<String, String> landscape)
	{
		this.landscape = landscape;
	}

	public Map<String, String> getLogo()
	{
		return logo;
	}

	public void setLogo(Map<String, String> logo)
	{
		this.logo = logo;
	}

	public Map<String, String> getModeration()
	{
		return moderation;
	}

	public void setModeration(Map<String, String> moderation)
	{
		this.moderation = moderation;
	}

	public Map<String, String> getPortraitQuality()
	{
		return portraitQuality;
	}

	public void setPortraitQuality(Map<String, String> portraitQuality)
	{
		this.portraitQuality = portraitQuality;
	}

	public String getIsBlackAndWhite()
	{
		return isBlackAndWhite;
	}

	public void setIsBlackAndWhite(String isBlackAndWhite)
	{
		this.isBlackAndWhite = isBlackAndWhite;
	}

	public Map<String, String> getImageType()
	{
		return imageType;
	}

	public void setImageType(Map<String, String> imageType)
	{
		this.imageType = imageType;
	}

	public Map<String, String> getLandmarks()
	{
		return landmarks;
	}

	public void setLandmarks(Map<String, String> landmarks)
	{
		this.landmarks = landmarks;
	}

	public Map<String, String> getNswf()
	{
		return nswf;
	}

	public void setNswf(Map<String, String> nswf)
	{
		this.nswf = nswf;
	}

	public String getEntities()
	{
		return entities;
	}

	public void setEntities(String entities)
	{
		this.entities = entities;
	}

	public String getFaceRaw()
	{
		return faceRaw;
	}

	public void setFaceRaw(String faceRaw)
	{
		this.faceRaw = faceRaw;
	}

	public Map<String, String> getOcrInfo()
	{
		return ocrInfo;
	}

	public void setOcrInfo(Map<String, String> ocrInfo)
	{
		this.ocrInfo = ocrInfo;
	}

	public Map<String, String> getTagsInfoDe()
	{
		return tagsInfoDe;
	}

	public void setTagsInfoDe(Map<String, String> tagsInfoDe)
	{
		this.tagsInfoDe = tagsInfoDe;
	}

	public Map<String, String> getCelebrityInfo()
	{
		return celebrityInfo;
	}

	public void setCelebrityInfo(Map<String, String> celebrityInfo)
	{
		this.celebrityInfo = celebrityInfo;
	}

	public String getRawData()
	{
		return rawData;
	}

	public void setRawData(String rawData)
	{
		this.rawData = rawData;
	}

	public Map<String, String> getTagsInfo()
	{
		return tagsInfo;
	}

	public void setTagsInfo(Map<String, String> tagsInfo)
	{
		this.tagsInfo = tagsInfo;
	}

	public Map<String, String> getCategoryInfo()
	{
		return categoryInfo;
	}

	public void setCategoryInfo(Map<String, String> categoryInfo)
	{
		this.categoryInfo = categoryInfo;
	}

	public Map<String, String> getColorInfo()
	{
		return colorInfo;
	}

	public void setColorInfo(Map<String, String> colorInfo)
	{
		this.colorInfo = colorInfo;
	}

	public Map<String, String> getCaptionInfo()
	{
		return captionInfo;
	}

	public void setCaptionInfo(Map<String, String> captionInfo)
	{
		this.captionInfo = captionInfo;
	}

	public Map<String, String> getFaceInfo()
	{
		return faceInfo;
	}

	public void setFaceInfo(Map<String, String> faceInfo)
	{
		this.faceInfo = faceInfo;
	}

	public String getStatusInfo()
	{
		return statusInfo;
	}

	public void setStatusInfo(String statusInfo)
	{
		this.statusInfo = statusInfo;
	}

	public List<String> getKeyPhrases()
	{
		return keyPhrases;
	}

	public void setKeyPhrases(List<String> keyPhrases)
	{
		this.keyPhrases = keyPhrases;
	}

	public String getLanguage()
	{
		return language;
	}

	public void setLanguage(String language)
	{
		this.language = language;
	}

	public String getSentiment()
	{
		return sentiment;
	}

	public void setSentiment(String sentiment)
	{
		this.sentiment = sentiment;
	}

	public List<String> getLinkedEntities()
	{
		return linkedEntities;
	}

	public void setLinkedEntities(List<String> linkedEntities)
	{
		this.linkedEntities = linkedEntities;
	}

	public String getEmotions()
	{
		return emotions;
	}

	public void setEmotions(String emotions)
	{
		this.emotions = emotions;
	}

	public String getNode()
	{
		return node;
	}

	public void setNode(String node)
	{
		this.node = node;
	}

	public String getClassificationCustom() {
		return classificationCustom;
	}

	public void setClassificationCustom(String classificationCustom) {
		this.classificationCustom = classificationCustom;
	}
}
