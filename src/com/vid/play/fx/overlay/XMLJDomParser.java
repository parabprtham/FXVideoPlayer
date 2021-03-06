package com.vid.play.fx.overlay;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ebml.BinaryElement;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import com.vid.matroska.MatroskaContainer;
import com.vid.overlay.comp.master.COMPONENT_TYPE;
import com.vid.overlay.comp.master.SHAPE_TYPE;
import com.vid.tagging.KeyWord;
import com.vid.tagging.VideoTag;

/**
 * @author pratham
 * 
 *         This class is used to parse the information from the xmlfile and
 *
 */
public class XMLJDomParser {

	private Map<AnnotationKey, Annotation> annotationsMap;
	private Map<Integer, KeyWord> keyWordsMap;
	private Map<Integer, List<VideoTag>> keyTagMap;
	private Map<Integer, VideoTag> tagMap;

	public XMLJDomParser() {
		annotationsMap = new HashMap<AnnotationKey, Annotation>();
		keyWordsMap = new HashMap<>();
		tagMap = new HashMap<>();
		keyTagMap = new HashMap<>();
	}

	/**
	 * @param fileName
	 * @return Map<AnnotationKey, Annotation>
	 * 
	 *         This method is used for reading the annotation from the XML file
	 *         and then generates a Annotation class object which holds the
	 *         start_time, end_time and instance of the actual component
	 *         class(Jcomponent) or informations regarding the parameters in
	 *         case of the static components.
	 * 
	 *         The map generated is consumed by the Overlay_genrated_factory to
	 *         create the Overlays to be displayed at run time
	 * 
	 */

	public Map<AnnotationKey, Annotation> xmlQuery(String fileName) {

		File inputFile = new File(fileName);
		return xmlQuery(inputFile);
	}

	public Map<AnnotationKey, Annotation> xmlQuery(MatroskaContainer container) {

		File inputFile = null;
		try {
			BinaryElement overlayFileData = container.getFileData(container.getOverlayFile());

			if (overlayFileData == null)
				return null;

			inputFile = new File(container.getOverlayFile().getValue());
			FileOutputStream fileOutputStream = new FileOutputStream(inputFile);
			fileOutputStream.write(overlayFileData.getData().array());
			fileOutputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return xmlQuery(inputFile);
	}

	public Map<AnnotationKey, Annotation> xmlQuery(File inputFile) {
		try {

			if (inputFile == null)
				return null;

			// System.out.println("Fetching elements for :" + inputFile);
			SAXBuilder saxBuilder = new SAXBuilder();
			Document document = saxBuilder.build(inputFile);
			System.out.println("Root element :" + document.getRootElement().getName());
			Element classElement = document.getRootElement();
			System.out.println("------------xmlQuery----------------");
			/*
			 * List<Element> videoDetails =
			 * classElement.getChildren("video_details"); Element
			 * supercarElement = videoDetails.get(0); System.out.println(
			 * "Current Element :" + supercarElement.getName()); Element
			 * file_name = supercarElement.getChild("file_name");
			 * System.out.println(file_name.getText().replace("\n", ""));
			 * Element file_hash = supercarElement.getChild("file_hash");
			 * System.out.println("file_name : " + file_hash.getText());
			 */
			System.out.println("------------Getting Annotations----------------");
			Element video_anno_data = classElement.getChild("Video_anno_data");
			List<Element> annotations = video_anno_data.getChildren("annotation");
			for (Element annotation : annotations) {
				// System.out.println(" " + annotation.getName());
				String id = annotation.getAttributeValue("id");
				// System.out.println(" id " + id);
				String className = annotation.getAttributeValue("type");
				// System.out.println(" ClassName " + className);
				String startTime = annotation.getChildTextTrim("starttime");
				// System.out.println(" StartTime " + startTime);
				String endTime = annotation.getChildTextTrim("endtime");
				// System.out.println(" Endtime " + endTime);
				AnnotationKey key = new AnnotationKey(Integer.parseInt(id), Integer.parseInt(startTime),
						Integer.parseInt(endTime));
				String comp_type = annotation.getChildTextTrim("comp_type");
				// System.out.println(" Comp_type " + comp_type);
				Element parameters = annotation.getChild("parameters");

				Element shape_type = annotation.getChild("static_component");
				SHAPE_TYPE shape_TYPE = null;
				if (shape_type != null)
					shape_TYPE = SHAPE_TYPE
							.getFromName(annotation.getChild("static_component").getAttribute("type").getValue());

				Annotation ann = new Annotation(Integer.parseInt(id), Integer.parseInt(startTime),
						Integer.parseInt(endTime), className, parameters, COMPONENT_TYPE.getFromName(comp_type),
						shape_TYPE);
						// System.out.println(" Parameters " + parameters);
						// System.out.println("----------------------------");

				// Commented as to move to the customoverlay
				// createComponent(parameters, annotation, className, comp_type,
				// id);
				// ann.setComp(component);

				annotationsMap.put(key, ann);
				// System.out.println("----------------------------");
			}

			System.out.println("------------Getting KeyWords----------------");

			Element video_tag_data = classElement.getChild("Video_tag_data");
			Element KeyWords = video_tag_data.getChild("KeyWords");
			List<Element> keywords = KeyWords.getChildren("keyword");
			for (Element keyword : keywords) {
				String id = keyword.getAttributeValue("id");
				Element parameters = keyword.getChild("parameters");
				Element word = parameters.getChild("Word");
				KeyWord keyWord2 = new KeyWord();
				keyWord2.setId(Integer.parseInt(id));
				keyWord2.setWord(word.getText());
				keyWordsMap.put(keyWord2.getId(), keyWord2);
			}

			System.out.println("------------Getting Tags----------------");
			Element Video_tags = video_tag_data.getChild("Video_tags");
			List<Element> videotags = Video_tags.getChildren("videotag");
			for (Element videotag : videotags) {
				String id = videotag.getAttributeValue("id");
				Element starttime = videotag.getChild("starttime");
				Element endtime = videotag.getChild("endtime");
				Element parameters = videotag.getChild("parameters");
				Element tagDescription = parameters.getChild("TagDescription");
				Element tagKeyWords = parameters.getChild("TagKeyWords");
				VideoTag tag = new VideoTag();
				tag.setSegmentId(Integer.parseInt(id));
				tag.setStartTime(Double.parseDouble(starttime.getText()));
				tag.setEndTime(Double.parseDouble(endtime.getText()));
				tag.setTagDescription(tagDescription.getText());
				String[] keyWordIds = tagKeyWords.getText().split(",");
				for (String keyWord : keyWordIds) {
					int keyId = Integer.parseInt(keyWord);
					tag.addKeyWord(keyWordsMap.get(keyId));
					addToTagMap(keyId, tag);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return annotationsMap;

	}

	private void addToTagMap(Integer key, VideoTag tag) {
		List<VideoTag> list = keyTagMap.get(key);
		if (list == null)
			list = new ArrayList<>();
		list.add(tag);
		keyTagMap.put(key, list);

	}

	public class Annotation implements Comparable<Annotation> {

		private int id;
		private int startTime;
		private int endTime;
		private Element parameters;
		private String className;
		private COMPONENT_TYPE component_TYPE;
		private SHAPE_TYPE shape_type;

		private boolean isClosed = false;

		public Annotation(int id, int startTime, int endTime, String className, Element parameters,
				COMPONENT_TYPE component_TYPE, SHAPE_TYPE shape_TYPE) {
			this.id = id;
			this.startTime = startTime;
			this.endTime = endTime;
			this.parameters = parameters;
			this.className = className;
			this.shape_type = shape_TYPE;
			this.component_TYPE = component_TYPE;
			this.isClosed = false;
		}

		@Override
		public int compareTo(Annotation a) {
			return this.startTime - a.startTime;
		}

		public int getStartTime() {
			return startTime;
		}

		public void setStartTime(int startTime) {
			this.startTime = startTime;
		}

		public int getEndTime() {
			return endTime;
		}

		public void setEndTime(int endTime) {
			this.endTime = endTime;
		}

		public COMPONENT_TYPE getComponent_TYPE() {
			return component_TYPE;
		}

		public void setComponent_TYPE(COMPONENT_TYPE component_TYPE) {
			this.component_TYPE = component_TYPE;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public Element getParameters() {
			return parameters;
		}

		public void setParameters(Element parameters) {
			this.parameters = parameters;
		}

		public String getClassName() {
			return className;
		}

		public void setClassName(String className) {
			this.className = className;
		}

		public SHAPE_TYPE getShape_type() {
			return shape_type;
		}

		public void setShape_type(SHAPE_TYPE shape_type) {
			this.shape_type = shape_type;
		}

		public boolean isClosed() {
			return isClosed;
		}

		public void setClosed(boolean isClosed) {
			this.isClosed = isClosed;
		}
	}

	public class AnnotationKey implements Comparable<AnnotationKey> {
		int id;
		int startTime;
		int endTime;
		boolean isChecked = false;

		public AnnotationKey(int id, int startTime, int endTime) {
			this.id = id;
			this.startTime = startTime;
			this.endTime = endTime;
		}

		@Override
		public int compareTo(AnnotationKey a) {
			return this.startTime - a.startTime;
		}

		public boolean equals(Object other) {
			if (!(other instanceof AnnotationKey)) {
				return false;
			}
			AnnotationKey key = (AnnotationKey) other;
			return key.id == id && key.startTime == startTime && key.endTime == endTime;
		}

		public int hashCode() {
			return id * 37 + startTime;
		}

		public int getStartTime() {
			return startTime;
		}

		public void setStartTime(int startTime) {
			this.startTime = startTime;
		}

		public int getEndTime() {
			return endTime;
		}

		public void setEndTime(int endTime) {
			this.endTime = endTime;
		}

		public boolean isChecked() {
			return isChecked;
		}

		public void setChecked(boolean isChecked) {
			this.isChecked = isChecked;
		}
	}

	public Map<AnnotationKey, Annotation> getAnnotationsMap() {
		return annotationsMap;
	}

	public void setAnnotationsMap(Map<AnnotationKey, Annotation> annotationsMap) {
		this.annotationsMap = annotationsMap;
	}

	public static void main(String[] args) {

		// readXml();
		XMLJDomParser domParserTest = new XMLJDomParser();
		domParserTest.xmlQuery("K:/Test/Out/test1_50.xml");
	}

	public Map<Integer, List<VideoTag>> getKeyTagMap() {
		return keyTagMap;
	}

	public void setKeyTagMap(Map<Integer, List<VideoTag>> keyTagMap) {
		this.keyTagMap = keyTagMap;
	}

	public Map<Integer, KeyWord> getKeyWordsMap() {
		return keyWordsMap;
	}

	public void setKeyWordsMap(Map<Integer, KeyWord> keyWordsMap) {
		this.keyWordsMap = keyWordsMap;
	}

	public Map<Integer, VideoTag> getTagMap() {
		return tagMap;
	}

	public void setTagMap(Map<Integer, VideoTag> tagMap) {
		this.tagMap = tagMap;
	}

}