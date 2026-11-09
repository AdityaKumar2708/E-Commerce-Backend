package com.rkind.ecommerce.notification.repository;

import com.rkind.ecommerce.notification.entity.OrderNotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderNotificationSettingRepository
        extends JpaRepository<OrderNotificationSetting, Long> {
}