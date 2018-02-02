package com.innovators.Controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.innovators.Entity.Candidate;
import com.innovators.repository.CandidateJPARepository;

@Controller
@Configuration
@PropertySource("classpath:skills.properties")
public class JDLoopUpController {
	
	@Value("${jdlookup.jdlabel}")
	private String jdlabel;
	
	@Value("${skills.list}")
	private String skillList;
	
	@RequestMapping("/jdlookup")
	public String welcome(Model model) {
		model.addAttribute("jdlookup.jdlabel", this.jdlabel);
		return "JDLookup";
	}
	
	@Autowired
	private CandidateJPARepository candidateJPARepository;
	
	@RequestMapping("/lookupskills")
	public ModelAndView insertCandidate(@RequestParam("jd") String jd,
			RedirectAttributes redirectAttributes) {
				
		ModelAndView modelandview = new ModelAndView("JDLookup");
		
		String role = getRole(jd);
		System.out.println("role is "+role);
		List<Candidate> candidates= candidateJPARepository.findByRole(role);
		
		modelandview.addObject("candidates", candidates);
		
		return modelandview;
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
