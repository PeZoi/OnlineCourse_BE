package com.springboot.courses.service;

import com.springboot.courses.payload.course.CourseReturnHomePageResponse;
import com.springboot.courses.payload.course.CourseReturnLearningPageResponse;
import com.springboot.courses.payload.course.CourseReturnMyLearning;
import com.springboot.courses.payload.quiz.QuizReturnLearningPage;
import com.springboot.courses.payload.video.VideoReturnResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface LearningService {

    CourseReturnLearningPageResponse getCourseReturnLearningPage(String slug);
    VideoReturnResponse getVideo(String slug, Integer lessonId);
    List<QuizReturnLearningPage> getQuiz(String slug, Integer lessonId);
    List<CourseReturnMyLearning> listAllCourseRegisteredByCustomer(HttpServletRequest request);
    boolean isRegisterInThisCourse(String slug, HttpServletRequest request);
}
