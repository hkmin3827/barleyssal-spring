package com.hakyung.barleyssal_spring.interfaces.account;

import com.hakyung.barleyssal_spring.application.account.AccountService;
import com.hakyung.barleyssal_spring.application.account.dto.AccountResponse;
import com.hakyung.barleyssal_spring.application.account.dto.HoldingResponse;
import com.hakyung.barleyssal_spring.application.account.dto.SetPrincipalRequest;
import com.hakyung.barleyssal_spring.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /** 내 계좌 조회 */
    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyAccount(
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(accountService.getOrCreateAccount(user.getId(), user.getUserName()));
    }

    /** 원금 설정 */
    @PutMapping("/set-principal")
    public ResponseEntity<AccountResponse> setPrincipal(
        @AuthenticationPrincipal CustomUserDetails user,
        @Valid @RequestBody SetPrincipalRequest req
    ) {
        return ResponseEntity.ok(accountService.setPrincipal(user.getId(), req));
    }

    /** 보유 종목 리스트 */
    @GetMapping("/me/holdings")
    public ResponseEntity<List<HoldingResponse>> getHoldings(
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(accountService.getHoldings(user.getId()));
    }
}
