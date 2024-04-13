package com.springboot.courses.service;

import com.springboot.courses.payload.track.InfoCourseRegistered;

public interface TrackCourseService {
    InfoCourseRegistered listTrackCourse(String email , String slug);
    Integer confirmLessonLearned(String email , Integer lessonIdPre);
}
