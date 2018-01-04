package com.dump.service;

import com.dump.service.controllers.DumpAPIController;
import com.dump.service.objects.Dump;
import com.dump.service.objects.User;
import com.dump.service.repositories.DumpRepository;
import com.dump.service.repositories.UserRepository;
import com.dump.service.utils.Auth;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Tests Dump API REST controller
 */
@RunWith(SpringRunner.class)
@WebMvcTest(DumpAPIController.class)
public class DumpAPIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DumpRepository dumpRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private Auth authUtil;

    @Test
    public void checkView() throws Exception {

        String string = "January 1, 2050";
        DateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);

        Dump mockDump = new Dump();
        mockDump.setExpiration(dateFormat.parse(string));
        mockDump.setViews(1);
        mockDump.setUsername("josh");
        mockDump.setContents("contents");
        mockDump.setPublicId("aaaa");

        User mockUser = new User();
        mockUser.setUsername("josh");
        mockUser.setViews(2);

        // verify 404 on invalid Dump
        when(dumpRepository.findByPublicId("aaaa"))
                .thenReturn(null);

        this.mockMvc
                .perform(get("/api/dumps/view/aaaa"))
                .andDo(print())
                .andExpect(status().isNotFound());

        // verify valid Dump request
        when(dumpRepository.findByPublicId("aaaa"))
                .thenReturn(mockDump);

        when(userRepository.findByUsernameIgnoreCase("josh"))
                .thenReturn(mockUser);

        this.mockMvc
                .perform(get("/api/dumps/view/aaaa"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("josh"))
                .andExpect(jsonPath("$.views").value(2));

        // verify user views were updated
        assertThat(mockUser.getViews() == 3);

        // verify download method
        this.mockMvc
                .perform(get("/api/dumps/view/aaaa?download=true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain"))
                .andExpect(header().string("Content-disposition", "attachment; filename=\"aaaa.txt\""))
                .andExpect(content().string("contents"));
    }
}
