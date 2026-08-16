@Entity
@Table(name="integration_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor 

public class IntegrationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_member_id", nullable = false)
    private UUID workspaceMemberId;

    @Column(name = "provider")
    private String provider;

    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp 
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp()
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


}