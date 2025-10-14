package io.resume.make.domain.user.service;

import io.resume.make.domain.auth.service.KakaoOAuthService;
import io.resume.make.domain.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {
    private final KakaoOAuthService kakaoOAuthService;
    private final UserRepository userRepository;

}
