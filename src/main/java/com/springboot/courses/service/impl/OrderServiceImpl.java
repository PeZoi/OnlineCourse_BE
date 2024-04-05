package com.springboot.courses.service.impl;

import com.springboot.courses.entity.Courses;
import com.springboot.courses.entity.Order;
import com.springboot.courses.entity.User;
import com.springboot.courses.exception.BlogApiException;
import com.springboot.courses.exception.ResourceNotFoundException;
import com.springboot.courses.payload.order.OrderRequest;
import com.springboot.courses.payload.order.OrderResponse;
import com.springboot.courses.repository.CoursesRepository;
import com.springboot.courses.repository.OrderRepository;
import com.springboot.courses.repository.UserRepository;
import com.springboot.courses.service.OrderService;
import com.springboot.courses.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CoursesRepository coursesRepository;
    @Autowired private ModelMapper modelMapper;

    @Override
    public OrderResponse createOrder(OrderRequest orderRequest, HttpServletRequest servletRequest) {
        String email = Utils.getEmailOfAuthenticatedCustomer(servletRequest);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Courses courses = coursesRepository.findById(orderRequest.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", orderRequest.getCourseId()));

        if(orderRepository.existsOrderByCoursesAndUser(courses, user)){
            throw new BlogApiException(HttpStatus.BAD_REQUEST, "Customer have purchased this course");
        }

        Order order = new Order();
        order.setUser(user);
        order.setCreatedTime(new Date());
        order.setCourses(courses);
        order.setTotalPrice(orderRequest.getTotalPrice());

        Order savedOrder = orderRepository.save(order);

        int totalStudent = courses.getStudentCount();
        courses.setStudentCount(totalStudent + 1);
        coursesRepository.save(courses);

        OrderResponse orderResponse = modelMapper.map(savedOrder, OrderResponse.class);
        orderResponse.setCustomerName(savedOrder.getUser().getFullName());
        orderResponse.setCourseName(savedOrder.getCourses().getTitle());
        return orderResponse;
    }



    @Override
    public List<OrderResponse> getAll() {
        List<Order> listOrders = orderRepository.findAll();
        List<OrderResponse> listOrderResponse = new ArrayList<>();

        for (Order order : listOrders){
            OrderResponse response = modelMapper.map(order, OrderResponse.class);
            response.setCourseName(order.getCourses().getTitle());
            response.setCustomerName(order.getUser().getFullName());
            listOrderResponse.add(response);
        }
        return listOrderResponse;
    }

    @Override
    public String deleteOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        Integer courseId = order.getCourses().getId();
        orderRepository.delete(order);
        Courses courses = coursesRepository.findById(courseId).get();
        int totalStudent = courses.getStudentCount() - 1;
        courses.setStudentCount(totalStudent);
        coursesRepository.save(courses);
        return "Delete order successfully ";
    }


}