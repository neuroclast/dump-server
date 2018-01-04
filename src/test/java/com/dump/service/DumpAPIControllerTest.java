package com.dump.service;

import com.dump.service.controllers.DumpAPIController;
import com.dump.service.objects.Dump;
import com.dump.service.objects.User;
import com.dump.service.repositories.DumpRepository;
import com.dump.service.repositories.UserRepository;
import com.dump.service.utils.Auth;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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


    /**
     * Tests /view handler
     * @throws Exception
     */
    @Test
    public void testView() throws Exception {

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

        // configure mocks
        when(dumpRepository.findByPublicId("aaaa"))
                .thenReturn(null)       // invalid Dump
                .thenReturn(mockDump);  // valid Dump

        when(userRepository.findByUsernameIgnoreCase("josh"))
                .thenReturn(mockUser);  // valid User

        // verify 404 on invalid Dump
        this.mockMvc
                .perform(get("/api/dumps/view/aaaa"))
                .andDo(print())
                .andExpect(status().isNotFound());

        // verify valid Dump request
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


    /**
     * Tests /delete handler
     * @throws Exception
     */
    @Test
    public void testDelete() throws Exception {
        Dump mockDump = new Dump();
        mockDump.setUsername("Josh");
        User wrongMockUser = new User();
        wrongMockUser.setUsername("not-josh");
        User mockUser = new User();
        mockUser.setUsername("josh");

        when(authUtil.verifyAuthorization(any()))
                .thenThrow(new Exception("expired"))    // expired JWT
                .thenReturn(null)                       // invalid user
                .thenReturn(wrongMockUser)              // unauthorized user
                .thenReturn(wrongMockUser)              // unauthorized user
                .thenReturn(mockUser);                  // authorized user

        when(dumpRepository.findByPublicId("aaaa"))
                .thenReturn(null)                       // invalid Dump
                .thenReturn(mockDump);                  // valid Dump

        // test expired JWT
        this.mockMvc
                .perform(delete("/api/dumps/delete?publicId=aaaa"))
                .andDo(print())
                .andExpect(status().isIAmATeapot());

        // test invalid user
        this.mockMvc
                .perform(delete("/api/dumps/delete?publicId=aaaa"))
                .andDo(print())
                .andExpect(status().isForbidden());

        // test invalid dump
        this.mockMvc
                .perform(delete("/api/dumps/delete?publicId=aaaa"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // test unauthorized user
        this.mockMvc
                .perform(delete("/api/dumps/delete?publicId=aaaa"))
                .andDo(print())
                .andExpect(status().isForbidden());

        // test success
        this.mockMvc
                .perform(delete("/api/dumps/delete?publicId=aaaa"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(authUtil, times(5)).verifyAuthorization(any());
        verify(dumpRepository, times(1)).delete(any(Dump.class));

    }


    /**
     * Tests /update handler
     * @throws Exception
     */
    @Test
    public void testUpdate() throws Exception {
        Dump mockDump = new Dump();
        mockDump.setUsername("Josh");
        mockDump.setTitle("2018-01-04 15:04:11.455 DEBUG 21248 --- [       Thread-2] o.s.b.f.s.DefaultListableBeanFactory     : Retrieved dependent beans for bean 'org.springframework.boot.autoconfigure.internalCachingMetadataReaderFactory': [org.springframework.context.annotation.internalConfigurationAnnotationProcessor]");
        mockDump.setPublicId("aaaa");
        User mockUser = new User();
        mockUser.setUsername("josh");

        when(authUtil.verifyAuthorization(any()))
                .thenThrow(new Exception("expired"))    // expired JWT
                .thenReturn(null)                       // invalid user
                .thenReturn(mockUser);                  // value user

        // test expired JWT
        this.mockMvc
                .perform(
                        post("/api/dumps/update")
                            .content(asJsonString(mockDump))
                            .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isIAmATeapot());

        // test invalid user
        this.mockMvc
                .perform(
                        post("/api/dumps/update")
                            .content(asJsonString(mockDump))
                            .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        // test success with culled title
        this.mockMvc
                .perform(
                        post("/api/dumps/update")
                                .content(asJsonString(mockDump))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("aaaa"));

        assertThat(mockDump.getTitle().length() == 250);
        verify(dumpRepository, times(1)).save(any(Dump.class));
    }


    /**
     * Tests /add handler
     * @throws Exception
     */
    @Test
    public void testAdd() throws Exception {
        Dump mockDump = new Dump();
        mockDump.setUsername("Josh");
        mockDump.setTitle("2018-01-04 15:04:11.455 DEBUG 21248 --- [       Thread-2] o.s.b.f.s.DefaultListableBeanFactory     : Retrieved dependent beans for bean 'org.springframework.boot.autoconfigure.internalCachingMetadataReaderFactory': [org.springframework.context.annotation.internalConfigurationAnnotationProcessor]");
        mockDump.setPublicId("aaaa");
        User mockUser = new User();
        mockUser.setUsername("josh");

        when(authUtil.verifyAuthorization(any()))
                .thenThrow(new Exception("expired"))    // expired JWT
                .thenReturn(null)                       // invalid user
                .thenReturn(mockUser);                  // success

        when(dumpRepository.findByPublicId(any()))
                .thenReturn(null);                      // unique public ID

        // test expired JWT
        this.mockMvc
                .perform(
                        post("/api/dumps/add")
                                .content(asJsonString(mockDump))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isIAmATeapot());

        // test invalid user
        this.mockMvc
                .perform(
                        post("/api/dumps/add")
                                .content(asJsonString(mockDump))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        // test success with culled title
        this.mockMvc
                .perform(
                        post("/api/dumps/add")
                                .content(asJsonString(mockDump))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        assertThat(mockDump.getPublicId().length() == 5);
        assertThat(mockDump.getTitle().length() <= 250);
        verify(dumpRepository, times(1)).save(any(Dump.class));
    }


    /**
     * Tests /user handler
     * @throws Exception
     */
    @Test
    public void testUser() throws Exception {
        User mockUser = new User();
        mockUser.setUsername("josh");

        when(authUtil.verifyAuthorization(any()))
                .thenThrow(new Exception("expired"))    // expired JWT
                .thenReturn(null)                       // unauthorized user
                .thenReturn(mockUser);                  // authorized user

        when(dumpRepository.findFirst100ByUsernameIgnoreCaseOrderByIdDesc("josh"))
                .thenReturn(new Dump[5])                // auth user
                .thenReturn(new Dump[5]);               // anon user

        // test expired JWT
        this.mockMvc
                .perform(get("/api/dumps/user?username=josh&viewAll=true"))
                .andDo(print())
                .andExpect(status().isIAmATeapot());

        // test unauthorized user
        this.mockMvc
                .perform(get("/api/dumps/user?username=josh&viewAll=true"))
                .andDo(print())
                .andExpect(status().isForbidden());

        // test authorized user
        this.mockMvc
                .perform(get("/api/dumps/user?username=josh&viewAll=true"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(dumpRepository, times(1)).findFirst100ByUsernameIgnoreCaseOrderByIdDesc(any());

        // test authorized user
        this.mockMvc
                .perform(get("/api/dumps/user?username=josh&viewAll=false"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(dumpRepository, times(1)).findFirst100ByUsernameIgnoreCaseAndExposureOrderByIdDesc(any(), any());
    }


    /**
     * Returns JSONified version of passed Object
     * @param obj Object to serialize to JSON
     * @return String representation of serialized object
     */
    private String asJsonString(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
