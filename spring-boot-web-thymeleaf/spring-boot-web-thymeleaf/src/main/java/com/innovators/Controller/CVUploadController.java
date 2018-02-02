package com.innovators.Controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.innovators.Entity.Candidate;
import com.innovators.repository.CandidateJPARepository;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

@Controller
@Configuration
@PropertySource("classpath:skills.properties")
public class CVUploadController {

	// inject via application.properties
	@Value("${welcome.message:test}")
	private String message;

	@Value("${skills.list}")
	private String skillList;

	@Autowired
	private CandidateJPARepository candidateJPARepository;

	@RequestMapping("/")
	public String welcome(Model model) {
		model.addAttribute("message", this.message);
		return "welcome";
	}

	@PostMapping("/upload")
	public ModelAndView insertCandidate(@RequestParam("file") MultipartFile file,
			RedirectAttributes redirectAttributes) {
		ModelAndView modelandview = new ModelAndView("welcome");
		try {
			Tika tika = new Tika();
			File f = convert(file);
			String filecontent = tika.parseToString(f);
			modelandview.addObject("message", "Uploaded successfully");			
			Candidate candidate = new Candidate();
			String cname = null;
			if (filecontent.length() > 200)
				cname = getName(filecontent.substring(1, 200));
			else
				cname = getName(filecontent);
			
			candidate.setName(cname); // Pending
			candidate.setEmail(getEmail(filecontent));
			candidate.setExperience(getExperience(filecontent));
			candidate.setSkills(saveSkills(filecontent));
			candidate.setCellNo(getCellNumber(Arrays.copyOfRange(
					filecontent.replace('\n', ' ').replace('\t', ' ').replaceAll(" +", " ").split(" "), 0, 20)));
			candidate.setRole(getRole(filecontent)); 

			candidateJPARepository.save(candidate);			
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TikaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return modelandview;
	}

	private String getEmail(String filecontent) {

		char[] separatorArray = { ' ', '\t', '\n', '\r', ':', '|' };
		int startIndex = 0;
		String firstStr = filecontent.substring(0, filecontent.indexOf("@"));

		int endIndex = (filecontent.indexOf("@")
				+ filecontent.substring(filecontent.indexOf("@")).indexOf(" ") > filecontent.indexOf("@")
						+ filecontent.substring(filecontent.indexOf("@")).indexOf("\n"))
								? filecontent.indexOf("@")
										+ filecontent.substring(filecontent.indexOf("@")).indexOf("\n")
								: filecontent.indexOf("@")
										+ filecontent.substring(filecontent.indexOf("@")).indexOf(" ");
		for (char ch : separatorArray) {
			startIndex = Math.max(firstStr.lastIndexOf(ch), startIndex);
		}
		String email = filecontent.substring(startIndex + 1, endIndex);
		/*System.out.println("Email id is " + email);*/
		return email;
	}

	private File convert(MultipartFile file) throws IOException {
		File convFile = new File(file.getOriginalFilename());
		convFile.createNewFile();
		FileOutputStream fos = new FileOutputStream(convFile);
		fos.write(file.getBytes());
		fos.close();
		return convFile;
	}

	private String getName(String text) {
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER,
		// parsing, and coreference resolution
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// read some text in the text variable
		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
		StringBuilder name = new StringBuilder();
		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
				// this is the text of the token
				String word = token.get(CoreAnnotations.TextAnnotation.class);
				// this is the POS tag of the token
				String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
				// this is the NER label of the token
				String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

				// System.out.println(String.format("Print: word: [%s] pos: [%s] ne: [%s]",
				// word, pos, ne));
				if (pos.equalsIgnoreCase("NNP") && ne.equalsIgnoreCase("PERSON")) {
					name.append(word).append(" ");
				} else
					continue;
			}			
		}
		if (name.toString().equals("")) {
			for (String key: text.replaceAll(":","").replaceAll("Name", "").replaceAll("Resume", "").replaceAll("CV", "").split(" ")) {
				if (!checkForNumber(key)) {
					name.append(key).append(" ");
				}else break;
			}
		}
		return name.toString();		
	}

	private String getExperience(String filecontent) {
		int lastIndex = filecontent.substring(0, filecontent.indexOf("ears")).lastIndexOf(" ");
		int firstIndex = filecontent.substring(0, lastIndex).lastIndexOf(" ");
		String exp = filecontent.substring(firstIndex + 1, lastIndex);
		System.out.print(exp);
		// System.out.print(filecontent.substring(filecontent.indexOf("ears")-10,
		// filecontent.indexOf("ears")).replaceAll("[^0-9]", ""));
		System.out.println(" years of experience");
		return exp;
	}

	private String getCellNumber(String[] words) {
		StringBuffer cellNo = new StringBuffer();
		for (String word : words) {
			if (word.indexOf('@') == -1) {
				if (checkForNumber(word)) {
					cellNo.append(word);
				}
			}
		}
		System.out.println("Cell Number is " + cellNo);
		return cellNo.toString();
	}

	private boolean checkForNumber(String word) {
		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			if (Character.isDigit(c)) {
				return true;
			}
		}
		return false;
	}

	private String saveSkills(String filecontent) {
		String[] skills = skillList.split(",");
		String[] text = filecontent.replaceAll("\n", " ").replaceAll("\t", " ").replaceAll(",", " ")
				.replaceAll(" +", " ").split(" ");

		List<String> skillList = new ArrayList<String>();
		skillList = Arrays.asList(skills);
		InputStream input = null;
		StringBuilder sbSkills = new StringBuilder();
		try {
			String filename = "skills.properties";
			input = getClass().getClassLoader().getResourceAsStream(filename);
			if (input == null) {
				System.out.println("Sorry, unable to find " + filename);				
			}
			Properties properties = new Properties();
			properties.load(input);
			input.close();
			Set<String> candidateSkill = new HashSet<String>();
			for (String word : text) {
				if (skillList.contains(word.trim().replaceAll("-", "").toLowerCase())) {
					candidateSkill.add(word.trim().replaceAll("-", "").toLowerCase());
					// System.out.println(word+properties.getProperty(word.trim().toLowerCase()));
				}
			}			
			for (String skill : candidateSkill) {
				sbSkills.append(skill).append("|");				
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		System.out.println(sbSkills.toString());
		return sbSkills.toString();
	}
	
	private String getRole(String filecontent) {
		String[] skills = skillList.split(",");
		String[] text = filecontent.replaceAll("\n", " ").replaceAll("\t", " ").replaceAll(",", " ")
				.replaceAll(" +", " ").split(" ");

		List<String> skillList = new ArrayList<String>();
		skillList = Arrays.asList(skills);
		InputStream input = null;
		String roles;
		Map<String, Integer> roleCount = new HashMap<String, Integer>();
		try {
			String filename = "skills.properties";
			input = getClass().getClassLoader().getResourceAsStream(filename);
			if (input == null) {
				System.out.println("Sorry, unable to find " + filename);				
			}
			Properties properties = new Properties();
			properties.load(input);
			input.close();
			Set<String> candidateSkill = new HashSet<String>();
			for (String word : text) {
				if (skillList.contains(word.trim().replaceAll("-", "").toLowerCase())) {
					candidateSkill.add(word.trim().replaceAll("-", "").toLowerCase());
					// System.out.println(word+properties.getProperty(word.trim().toLowerCase()));
				}
			}			
			for (String skill : candidateSkill) {
				roles = properties.getProperty(skill);
				// String[] sl1= role.split(",");
				for (String role : roles.split(",")) {
					if (!roleCount.containsKey(role)) {
						roleCount.put(role, 1);
					} else {
						roleCount.put(role, roleCount.get(role) + 1);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String finalRole = null;
		int count = 0;
		for (String key: roleCount.keySet()) {
			if (roleCount.get(key)>count) {
				finalRole = key;
				count = roleCount.get(key);
			}
		}
		return finalRole;
    
	}

}