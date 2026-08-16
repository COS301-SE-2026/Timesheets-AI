@Entity
@Table(name = "integration_tokens")
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

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
