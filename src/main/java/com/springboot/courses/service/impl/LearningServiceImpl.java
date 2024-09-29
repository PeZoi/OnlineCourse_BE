package com.springboot.courses.service.impl;

import com.springboot.courses.entity.*;
import com.springboot.courses.exception.ResourceNotFoundException;
import com.springboot.courses.payload.TextLessonDto;
import com.springboot.courses.payload.chapter.ChapterReturnDetailResponse;
import com.springboot.courses.payload.course.CourseReturnLearningPageResponse;
import com.springboot.courses.payload.course.CourseReturnMyLearning;
import com.springboot.courses.payload.lesson.LessonReturnDetailResponse;
import com.springboot.courses.payload.quiz.QuizReturnLearningPage;
import com.springboot.courses.payload.video.VideoReturnResponse;
import com.springboot.courses.repository.*;
import com.springboot.courses.service.LearningService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LearningServiceImpl implements LearningService {

    @Autowired private CoursesRepository coursesRepository;
    @Autowired private ModelMapper modelMapper;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private TrackCourseRepository trackCourseRepository;
    @Autowired private TextLessonRepository textLessonRepository;

    @Override
    public CourseReturnLearningPageResponse getCourseReturnLearningPage(String slug) {
        Courses coursesInDB = coursesRepository.findCoursesBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "slug", slug));

        CourseReturnLearningPageResponse course = modelMapper.map(coursesInDB, CourseReturnLearningPageResponse.class);
        sortChapterAndLesson(course);
        return course;
    }

    @Override
    public VideoReturnResponse getVideo(Integer lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        Integer videoId = lesson.getVideo().getId();

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));

        return modelMapper.map(video, VideoReturnResponse.class);
    }

    @Override
    public List<QuizReturnLearningPage> getQuiz(Integer lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        List<Quiz> quizzes = lesson.getQuizList();
        return convertToQuizLearningPage(quizzes);
    }

    public List<QuizReturnLearningPage> convertToQuizLearningPage(List<Quiz> quizzes){
        List<QuizReturnLearningPage> listQuizzes = new ArrayList<>();
        for (Quiz quiz : quizzes){
            QuizReturnLearningPage quizReturnLearningPage = modelMapper.map(quiz, QuizReturnLearningPage.class);
            if(quiz.getQuizType().toString().equals("PERFORATE")){
                quizReturnLearningPage.setAnswerList(null);
            }
            listQuizzes.add(quizReturnLearningPage);
        }
        return listQuizzes;
    }

    @Override
    public TextLessonDto getText(Integer lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        Integer textId = lesson.getText().getId();

        TextLesson textLesson = textLessonRepository.findById(textId)
                .orElseThrow(() -> new ResourceNotFoundException("Text", "id", textId));

        return modelMapper.map(textLesson, TextLessonDto.class);
    }

    @Override
    public List<CourseReturnMyLearning> listAllCourseRegisteredByCustomer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        List<Order> listOrders = user.getListOrders();
        List<CourseReturnMyLearning> listCourses = new ArrayList<>();

        for(Order order : listOrders){
            Courses courses = order.getCourses();
            List<TrackCourse> listTrackCourses = trackCourseRepository.findAllByCoursesAndUser(courses, user);
            int totalLesson = 0;
            int totalLessonLearned = 0;
            int process = 0;
            for (TrackCourse trackCourse : listTrackCourses){
                if(trackCourse.isCompleted()){
                    ++totalLessonLearned;
                }
                ++totalLesson;
            }

            float percent = (float) (totalLessonLearned * 100) / totalLesson;
            process = Math.round(percent);

            CourseReturnMyLearning myLearning = modelMapper.map(courses, CourseReturnMyLearning.class);
            myLearning.setProcess(process);
            listCourses.add(myLearning);
        }

        return listCourses;
    }

    @Override
    public boolean isRegisterInThisCourse(String slug, String email) {
        if(email != null){
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User","email", email));

            Courses courses = coursesRepository.findCoursesBySlug(slug)
                    .orElseThrow(() -> new ResourceNotFoundException("Course", "slug", slug));

            return orderRepository.existsOrderByCoursesAndUser(courses, user);

        }
        return false;
    }

    private void sortChapterAndLesson(CourseReturnLearningPageResponse course) {
        int totalLessonInCourse = 0;

        List<ChapterReturnDetailResponse> chapterList = course.getChapterList().stream().
                map(chapter -> modelMapper.map(chapter, ChapterReturnDetailResponse.class))
                .sorted(Comparator.comparingInt(ChapterReturnDetailResponse::getOrders)).toList();

        int i = 1;
        for (ChapterReturnDetailResponse chapter : chapterList){
            List<LessonReturnDetailResponse> listLesson = chapter.getLessonList();
            listLesson.sort(Comparator.comparingInt(LessonReturnDetailResponse::getOrders));

            Duration durationInChapter = Duration.ZERO;

            for (LessonReturnDetailResponse lesson : listLesson){
                if (lesson.getLessonType().equals(LessonType.VIDEO)){
                    Lesson lessonInDB = lessonRepository.findById(lesson.getId()).get();
                    Video video = videoRepository.findById(lessonInDB.getVideo().getId()).get();
                    lesson.setDuration(video.getDuration());

                    durationInChapter = durationInChapter.plus(
                            Duration.ofMinutes(video.getDuration().getMinute())
                                    .plusSeconds(video.getDuration().getSecond())
                    );
                }else{
                    LocalTime time = LocalTime.of(0,1,0);
                    lesson.setDuration(time);

                    durationInChapter = durationInChapter.plus(
                            Duration.ofMinutes(1)
                    );
                }
                lesson.setOrders(i);
                ++i;
            }
            totalLessonInCourse += listLesson.size();
            chapter.setTotalLesson(listLesson.size());

            long hours = durationInChapter.toHours();
            long minutes = durationInChapter.toMinutes();
            long seconds = durationInChapter.minusMinutes(minutes).getSeconds();

            LocalTime localTime = LocalTime.of((int) hours, (int) minutes, (int) seconds);
            chapter.setDurationChapter(localTime);
        }
        course.setTotalLesson(totalLessonInCourse);
    }
}
