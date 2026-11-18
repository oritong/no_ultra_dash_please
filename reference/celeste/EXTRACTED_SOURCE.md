# Celeste 冲刺相关原始代码摘录

摘自 NoelFB/Celeste 的 `Source/Player/Player.cs`。Copyright (c) 2018 Noel Berry，原始代码采用 [MIT 许可证](upstream/LICENSE)。以下代码按原文件整行摘录，未改写函数体；行号见各节标题。它们是相互依赖的片段，不能单独编译。

固定提交：`1b0ce45c75e05649ae91b44a8bb6b196684e4352`。完整原文件：[upstream/Player.cs](upstream/Player.cs)；中文说明：[DASH_MECHANICS.md](DASH_MECHANICS.md)；行号索引：[SOURCE_MANIFEST.json](SOURCE_MANIFEST.json)。

## 移动、跳跃、冲刺、攀爬和平台继承常量

Player.cs:23-125 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L23-L125) · ID: constants

```csharp
        public const float MaxFall = 160f;
        private const float Gravity = 900f;
        private const float HalfGravThreshold = 40f;

        private const float FastMaxFall = 240f;
        private const float FastMaxAccel = 300f;

        public const float MaxRun = 90f;
        public const float RunAccel = 1000f;
        private const float RunReduce = 400f;
        private const float AirMult = .65f;

        private const float HoldingMaxRun = 70f;
        private const float HoldMinTime = .35f;

        private const float BounceAutoJumpTime = .1f;

        private const float DuckFriction = 500f;
        private const int DuckCorrectCheck = 4;
        private const float DuckCorrectSlide = 50f;

        private const float DodgeSlideSpeedMult = 1.2f;
        private const float DuckSuperJumpXMult = 1.25f;
        private const float DuckSuperJumpYMult = .5f;

        private const float JumpGraceTime = 0.1f;
        private const float JumpSpeed = -105f;
        private const float JumpHBoost = 40f;
        private const float VarJumpTime = .2f;
        private const float CeilingVarJumpGrace = .05f;
        private const int UpwardCornerCorrection = 4;
        private const float WallSpeedRetentionTime = .06f;

        private const int WallJumpCheckDist = 3;
        private const float WallJumpForceTime = .16f;
        private const float WallJumpHSpeed = MaxRun + JumpHBoost;

        public const float WallSlideStartMax = 20f;
        private const float WallSlideTime = 1.2f;

        private const float BounceVarJumpTime = .2f;
        private const float BounceSpeed = -140f;
        private const float SuperBounceVarJumpTime = .2f;
        private const float SuperBounceSpeed = -185f;

        private const float SuperJumpSpeed = JumpSpeed;
        private const float SuperJumpH = 260f;
        private const float SuperWallJumpSpeed = -160f;
        private const float SuperWallJumpVarTime = .25f;
        private const float SuperWallJumpForceTime = .2f;
        private const float SuperWallJumpH = MaxRun + JumpHBoost * 2;

        private const float DashSpeed = 240f;
        private const float EndDashSpeed = 160f;
        private const float EndDashUpMult = .75f;
        private const float DashTime = .15f;
        private const float DashCooldown = .2f;
        private const float DashRefillCooldown = .1f;
        private const int DashHJumpThruNudge = 6;
        private const int DashCornerCorrection = 4;
        private const int DashVFloorSnapDist = 3;
        private const float DashAttackTime = .3f;

        private const float BoostMoveSpeed = 80f;
        public const float BoostTime = .25f;

        private const float DuckWindMult = 0f;
        private const int WindWallDistance = 3;

        private const float ReboundSpeedX = 120f;
        private const float ReboundSpeedY = -120f;
        private const float ReboundVarJumpTime = .15f;

        private const float ReflectBoundSpeed = 220f;

        private const float DreamDashSpeed = DashSpeed;
        private const int DreamDashEndWiggle = 5;
        private const float DreamDashMinTime = .1f;

        public const float ClimbMaxStamina = 110;
        private const float ClimbUpCost = 100 / 2.2f;
        private const float ClimbStillCost = 100 / 10f;
        private const float ClimbJumpCost = 110 / 4f;
        private const int ClimbCheckDist = 2;
        private const int ClimbUpCheckDist = 2;
        private const float ClimbNoMoveTime = .1f;
        public const float ClimbTiredThreshold = 20f;
        private const float ClimbUpSpeed = -45f;
        private const float ClimbDownSpeed = 80f;
        private const float ClimbSlipSpeed = 30f;
        private const float ClimbAccel = 900f;
        private const float ClimbGrabYMult = .2f;
        private const float ClimbHopY = -120f;
        private const float ClimbHopX = 100f;
        private const float ClimbHopForceTime = .2f;
        private const float ClimbJumpBoostTime = .2f;
        private const float ClimbHopNoWindTime = .3f;

        private const float LaunchSpeed = 280f;
        private const float LaunchCancelThreshold = 220f;

        private const float LiftYCap = -130f;
        private const float LiftXCap = 250f;
```

## 状态编号

Player.cs:140-162 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L140-L162) · ID: states

```csharp
        public const int StNormal = 0;
        public const int StClimb = 1;
        public const int StDash = 2;
        public const int StSwim = 3;
        public const int StBoost = 4;
        public const int StRedDash = 5;
        public const int StHitSquash = 6;
        public const int StLaunch = 7;
        public const int StPickup = 8;
        public const int StDreamDash = 9;
        public const int StSummitLaunch = 10;
        public const int StDummy = 11;
        public const int StIntroWalk = 12;
        public const int StIntroJump = 13;
        public const int StIntroRespawn = 14;
        public const int StIntroWakeUp = 15;
        public const int StBirdDashTutorial = 16;
        public const int StFrozen = 17;
        public const int StReflectionFall = 18;
        public const int StStarFly = 19;
        public const int StTempleFall = 20;
        public const int StCassetteFly = 21;
        public const int StAttract = 22;
```

## 移动状态、计时器与命中箱

Player.cs:210-266 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L210-L266) · ID: movement-fields

```csharp
        private Hitbox hurtbox;
        private float jumpGraceTimer;
        public bool AutoJump;
        public float AutoJumpTimer;
        private float varJumpSpeed;
        private float varJumpTimer;
        private int forceMoveX;
        private float forceMoveXTimer;
        private int hopWaitX;   // If you climb hop onto a moving solid, snap to beside it until you get above it
        private float hopWaitXSpeed;
        private Vector2 lastAim;
        private float dashCooldownTimer;
        private float dashRefillCooldownTimer;
        public Vector2 DashDir;
        private float wallSlideTimer = WallSlideTime;
        private int wallSlideDir;
        private float climbNoMoveTimer;
        private Vector2 carryOffset;
        private Vector2 deadOffset;
        private float introEase;
        private float wallSpeedRetentionTimer; // If you hit a wall, start this timer. If coast is clear within this timer, retain h-speed
        private float wallSpeedRetained;
        private int wallBoostDir;
        private float wallBoostTimer;   // If you climb jump and then do a sideways input within this timer, switch to wall jump
        private float maxFall;
        private float dashAttackTimer;
        private List<ChaserState> chaserStates;
        private bool wasTired;
        private HashSet<Trigger> triggersInside;
        private float highestAirY;
        private bool dashStartedOnGround;
        private bool fastJump;
        private int lastClimbMove;
        private float noWindTimer;
        private float dreamDashCanEndTimer;
        private Solid climbHopSolid;
        private Vector2 climbHopSolidPosition;
        private SoundSource wallSlideSfx;
        private SoundSource swimSurfaceLoopSfx;
        private float playFootstepOnLand;
        private float minHoldTimer;
        public Booster CurrentBooster;
        private Booster lastBooster;
        private bool calledDashEvents;
        private int lastDashes;
        private Sprite sweatSprite;
        private int startHairCount;
        private bool launched;
        private float launchedTimer;
        private float dashTrailTimer;
        private List<ChaserStateSound> activeSounds = new List<ChaserStateSound>();
        private FMOD.Studio.EventInstance idleSfx;

        private readonly Hitbox normalHitbox = new Hitbox(8, 11, -4, -11);
        private readonly Hitbox duckHitbox = new Hitbox(8, 6, -4, -6);
        private readonly Hitbox normalHurtbox = new Hitbox(8, 9, -4, -11);
        private readonly Hitbox duckHurtbox = new Hitbox(8, 4, -4, -6);
```

## 状态机回调注册

Player.cs:315-346 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L315-L346) · ID: state-callbacks

```csharp
            // physics
            Collider = normalHitbox;
            hurtbox = normalHurtbox;
            onCollideH = OnCollideH;
            onCollideV = OnCollideV;

            // states
            StateMachine = new StateMachine(23);
            StateMachine.SetCallbacks(StNormal, NormalUpdate, null, NormalBegin, NormalEnd);
            StateMachine.SetCallbacks(StClimb, ClimbUpdate, null, ClimbBegin, ClimbEnd);
            StateMachine.SetCallbacks(StDash, DashUpdate, DashCoroutine, DashBegin, DashEnd);
            StateMachine.SetCallbacks(StSwim, SwimUpdate, null, SwimBegin, null);
            StateMachine.SetCallbacks(StBoost, BoostUpdate, BoostCoroutine, BoostBegin, BoostEnd);
            StateMachine.SetCallbacks(StRedDash, RedDashUpdate, RedDashCoroutine, RedDashBegin, RedDashEnd);
            StateMachine.SetCallbacks(StHitSquash, HitSquashUpdate, null, HitSquashBegin, null);
            StateMachine.SetCallbacks(StLaunch, LaunchUpdate, null, LaunchBegin, null);
            StateMachine.SetCallbacks(StPickup, null, PickupCoroutine, null, null);
            StateMachine.SetCallbacks(StDreamDash, DreamDashUpdate, null, DreamDashBegin, DreamDashEnd);
            StateMachine.SetCallbacks(StSummitLaunch, SummitLaunchUpdate, null, SummitLaunchBegin, null);
            StateMachine.SetCallbacks(StDummy, DummyUpdate, null, DummyBegin, null);
            StateMachine.SetCallbacks(StIntroWalk, null, IntroWalkCoroutine, null, null);
            StateMachine.SetCallbacks(StIntroJump, null, IntroJumpCoroutine, null, null);
            StateMachine.SetCallbacks(StIntroRespawn, null, null, IntroRespawnBegin, IntroRespawnEnd);
            StateMachine.SetCallbacks(StIntroWakeUp, null, IntroWakeUpCoroutine, null, null);
            StateMachine.SetCallbacks(StTempleFall, TempleFallUpdate, TempleFallCoroutine);
            StateMachine.SetCallbacks(StReflectionFall, ReflectionFallUpdate, ReflectionFallCoroutine, ReflectionFallBegin, ReflectionFallEnd);
            StateMachine.SetCallbacks(StBirdDashTutorial, BirdDashTutorialUpdate, BirdDashTutorialCoroutine, BirdDashTutorialBegin, null);
            StateMachine.SetCallbacks(StFrozen, FrozenUpdate, null, null, null);
            StateMachine.SetCallbacks(StStarFly, StarFlyUpdate, StarFlyCoroutine, StarFlyBegin, StarFlyEnd);
            StateMachine.SetCallbacks(StCassetteFly, CassetteFlyUpdate, CassetteFlyCoroutine, CassetteFlyBegin, CassetteFlyEnd);
            StateMachine.SetCallbacks(StAttract, AttractUpdate, null, AttractBegin, AttractEnd);
            Add(StateMachine);
```

## 帧开始时的落地探测

Player.cs:633-651 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L633-L651) · ID: ground-check

```csharp
                //Get ground
                if (StateMachine.State == StDreamDash)
                    onGround = OnSafeGround = false;
                else if (Speed.Y >= 0)
                {
                    Platform first = CollideFirst<Solid>(Position + Vector2.UnitY);
                    if (first == null)
                        first = CollideFirstOutside<JumpThru>(Position + Vector2.UnitY);

                    if (first != null)
                    {
                        onGround = true;
                        OnSafeGround = first.Safe;
                    }
                    else
                        onGround = OnSafeGround = false;
                }
                else
                    onGround = OnSafeGround = false;
```

## 攀爬跳后改方向的 Wall Boost

Player.cs:688-699 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L688-L699) · ID: wallboost

```csharp
                //Wall Boost
                if (wallBoostTimer > 0)
                {
                    wallBoostTimer -= Engine.DeltaTime;
                    if (moveX == wallBoostDir)
                    {
                        Speed.X = WallJumpHSpeed * moveX;
                        Stamina += ClimbJumpCost;
                        wallBoostTimer = 0;
                        sweatSprite.Play("idle");                       
                    }
                }
```

## 落地处理、Dash Attack、土狼时间、恢复冲刺和变高跳计时

Player.cs:701-744 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L701-L744) · ID: refill-timers

```csharp
                //After Dash
                if (onGround && StateMachine.State != StClimb)
                {
                    AutoJump = false;
                    Stamina = ClimbMaxStamina;
                    wallSlideTimer = WallSlideTime;
                }

                //Dash Attack
                if (dashAttackTimer > 0)
                    dashAttackTimer -= Engine.DeltaTime;

                //Jump Grace
                if (onGround)
                {
                    dreamJump = false;
                    jumpGraceTimer = JumpGraceTime;
                }
                else if (jumpGraceTimer > 0)
                    jumpGraceTimer -= Engine.DeltaTime;

                //Dashes
                {
                    if (dashCooldownTimer > 0)
                        dashCooldownTimer -= Engine.DeltaTime;

                    if (dashRefillCooldownTimer > 0)
                        dashRefillCooldownTimer -= Engine.DeltaTime;
                    else if (SaveData.Instance.AssistMode && SaveData.Instance.Assists.DashMode == Assists.DashModes.Infinite && !level.InCutscene)
                        RefillDash();
                    else if (!Inventory.NoRefills)
                    {
                        if (StateMachine.State == StSwim)
                            RefillDash();
                        else if (onGround)
                            if (CollideCheck<Solid, NegaBlock>(Position + Vector2.UnitY) || CollideCheckOutside<JumpThru>(Position + Vector2.UnitY))
                                if (!CollideCheck<Spikes>(Position) || (SaveData.Instance.AssistMode && SaveData.Instance.Assists.Invincible))
                                    RefillDash();
                    }
                }

                //Var Jump
                if (varJumpTimer > 0)
                    varJumpTimer -= Engine.DeltaTime;
```

## 方向输入、朝向、瞄准和碰墙保速

Player.cs:759-811 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L759-L811) · ID: facing-and-retention

```csharp
                //Force Move X
                if (forceMoveXTimer > 0)
                {
                    forceMoveXTimer -= Engine.DeltaTime;
                    moveX = forceMoveX;
                }
                else
                {
                    moveX = Input.MoveX.Value;
                    climbHopSolid = null;
                }

                //Climb Hop Solid Movement
                if (climbHopSolid != null && !climbHopSolid.Collidable)
                    climbHopSolid = null;
                else if (climbHopSolid != null && climbHopSolid.Position != climbHopSolidPosition)
                {
                    var move = climbHopSolid.Position - climbHopSolidPosition;
                    climbHopSolidPosition = climbHopSolid.Position;
                    MoveHExact((int)move.X);
                    MoveVExact((int)move.Y);
                }

                //Wind
                if (noWindTimer > 0)
                    noWindTimer -= Engine.DeltaTime;

                //Facing
                if (moveX != 0 && InControl
                    && StateMachine.State != StClimb && StateMachine.State != StPickup && StateMachine.State != StRedDash && StateMachine.State != StHitSquash)
                {
                    var to = (Facings)moveX;
                    if (to != Facing && Ducking)
                        Sprite.Scale = new Vector2(0.8f, 1.2f);
                    Facing = to;
                }

                //Aiming
                lastAim = Input.GetAimVector(Facing);

                //Wall Speed Retention
                if (wallSpeedRetentionTimer > 0)
                {
                    if (Math.Sign(Speed.X) == -Math.Sign(wallSpeedRetained))
                        wallSpeedRetentionTimer = 0;
                    else if (!CollideCheck<Solid>(Position + Vector2.UnitX * Math.Sign(wallSpeedRetained)))
                    {
                        Speed.X = wallSpeedRetained;
                        wallSpeedRetentionTimer = 0;
                    }
                    else
                        wallSpeedRetentionTimer -= Engine.DeltaTime;
                }
```

## 组件更新后：贴地、取消蹲姿、水平和垂直碰撞移动

Player.cs:908-935 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L908-L935) · ID: movement-order

```csharp
            base.Update();
            
            //Light Offset
            if (Ducking)
                Light.Position = duckingLightOffset;
            else
                Light.Position = normalLightOffset;

            //Jump Thru Assist
            if (!onGround && Speed.Y <= 0 && (StateMachine.State != StClimb || lastClimbMove == -1) && CollideCheck<JumpThru>() && !JumpThruBoostBlockedCheck())
                MoveV(JumpThruAssistSpeed * Engine.DeltaTime);

            //Dash Floor Snapping
            if (!onGround && DashAttacking && DashDir.Y == 0)
            {
                if (CollideCheck<Solid>(Position + Vector2.UnitY * DashVFloorSnapDist) || CollideCheckOutside<JumpThru>(Position + Vector2.UnitY * DashVFloorSnapDist))
                    MoveVExact(DashVFloorSnapDist);
            }

            //Falling unducking
            if (Speed.Y > 0 && CanUnDuck && Collider != starFlyHitbox && !onGround)
                Ducking = false;

            //Physics
            if (StateMachine.State != StDreamDash && StateMachine.State != StAttract)
                MoveH(Speed.X * Engine.DeltaTime, onCollideH);
            if (StateMachine.State != StDreamDash && StateMachine.State != StAttract)
                MoveV(Speed.Y * Engine.DeltaTime, onCollideV);
```

## Jump、SuperJump、WallJump、SuperWallJump、ClimbJump

Player.cs:1660-1842 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L1660-L1842) · ID: jump-family

```csharp
        public void Jump(bool particles = true, bool playSfx = true)
        {
            Input.Jump.ConsumeBuffer();
            jumpGraceTimer = 0;
            varJumpTimer = VarJumpTime;
            AutoJump = false;
            dashAttackTimer = 0;
            wallSlideTimer = WallSlideTime;
            wallBoostTimer = 0;

            Speed.X += JumpHBoost * moveX;
            Speed.Y = JumpSpeed;
            Speed += LiftBoost;
            varJumpSpeed = Speed.Y;

            LaunchedBoostCheck();

            if (playSfx)
            {
                if (launched)
                    Play(Sfxs.char_mad_jump_assisted);

                if (dreamJump)
                    Play(Sfxs.char_mad_jump_dreamblock);
                else
                    Play(Sfxs.char_mad_jump);
            }

            Sprite.Scale = new Vector2(.6f, 1.4f);
            if (particles)
                Dust.Burst(BottomCenter, Calc.Up, 4);

            SaveData.Instance.TotalJumps++;
        }

        private void SuperJump()
        {
            Input.Jump.ConsumeBuffer();
            jumpGraceTimer = 0;
            varJumpTimer = VarJumpTime;
            AutoJump = false;
            dashAttackTimer = 0;
            wallSlideTimer = WallSlideTime;
            wallBoostTimer = 0;

            Speed.X = SuperJumpH * (int)Facing;
            Speed.Y = JumpSpeed;
            Speed += LiftBoost;

            Play(Sfxs.char_mad_jump);

            if (Ducking)
            {
                Ducking = false;
                Speed.X *= DuckSuperJumpXMult;
                Speed.Y *= DuckSuperJumpYMult;
                Play(Sfxs.char_mad_jump_superslide);
            }
            else
                Play(Sfxs.char_mad_jump_super);

            varJumpSpeed = Speed.Y;
            launched = true;

            Sprite.Scale = new Vector2(.6f, 1.4f);

            Dust.Burst(BottomCenter, Calc.Up, 4);

            SaveData.Instance.TotalJumps++;
        }

        private bool WallJumpCheck(int dir)
        {
            return ClimbBoundsCheck(dir) && CollideCheck<Solid>(Position + Vector2.UnitX * dir * WallJumpCheckDist);
        }

        private void WallJump(int dir)
        {
            Ducking = false;
            Input.Jump.ConsumeBuffer();
            jumpGraceTimer = 0;
            varJumpTimer = VarJumpTime;
            AutoJump = false;
            dashAttackTimer = 0;
            wallSlideTimer = WallSlideTime;
            wallBoostTimer = 0;
            if (moveX != 0)
            {
                forceMoveX = dir;
                forceMoveXTimer = WallJumpForceTime;
            }

            //Get lift of wall jumped off of
            if (LiftSpeed == Vector2.Zero)
            {
                Solid wall = CollideFirst<Solid>(Position + Vector2.UnitX * WallJumpCheckDist);
                if (wall != null)
                    LiftSpeed = wall.LiftSpeed;
            }

            Speed.X = WallJumpHSpeed * dir;
            Speed.Y = JumpSpeed;
            Speed += LiftBoost;
            varJumpSpeed = Speed.Y;

            LaunchedBoostCheck();

            // wall-sound?
            var pushOff = SurfaceIndex.GetPlatformByPriority(CollideAll<Platform>(Position - Vector2.UnitX * dir * 4, temp));
            if (pushOff != null)
                Play(Sfxs.char_mad_land, SurfaceIndex.Param, pushOff.GetWallSoundIndex(this, -dir));

            // jump sfx
            Play(dir < 0 ? Sfxs.char_mad_jump_wall_right : Sfxs.char_mad_jump_wall_left);
            Sprite.Scale = new Vector2(.6f, 1.4f);

            if (dir == -1)
                Dust.Burst(Center + Vector2.UnitX * 2, Calc.UpLeft, 4);
            else
                Dust.Burst(Center + Vector2.UnitX * -2, Calc.UpRight, 4);

            SaveData.Instance.TotalWallJumps++;
        }

        private void SuperWallJump(int dir)
        {
            Ducking = false;
            Input.Jump.ConsumeBuffer();
            jumpGraceTimer = 0;
            varJumpTimer = SuperWallJumpVarTime;
            AutoJump = false;
            dashAttackTimer = 0;
            wallSlideTimer = WallSlideTime;
            wallBoostTimer = 0;

            Speed.X = SuperWallJumpH * dir;
            Speed.Y = SuperWallJumpSpeed;
            Speed += LiftBoost;
            varJumpSpeed = Speed.Y;
            launched = true;

            Play(dir < 0 ? Sfxs.char_mad_jump_wall_right : Sfxs.char_mad_jump_wall_left);
            Play(Sfxs.char_mad_jump_superwall);
            Sprite.Scale = new Vector2(.6f, 1.4f);

            if (dir == -1)
                Dust.Burst(Center + Vector2.UnitX * 2, Calc.UpLeft, 4);
            else
                Dust.Burst(Center + Vector2.UnitX * -2, Calc.UpRight, 4);

            SaveData.Instance.TotalWallJumps++;
        }

        private void ClimbJump()
        {
            if (!onGround)
            {
                Stamina -= ClimbJumpCost;

                sweatSprite.Play("jump", true);
                Input.Rumble(RumbleStrength.Light, RumbleLength.Medium);
            }

            dreamJump = false;
            Jump(false, false);

            if (moveX == 0)
            {
                wallBoostDir = -(int)Facing;
                wallBoostTimer = ClimbJumpBoostTime;
            }

            if (Facing == Facings.Right)
            {
                Play(Sfxs.char_mad_jump_climb_right);
                Dust.Burst(Center + Vector2.UnitX * 2, Calc.UpLeft, 4);
            }
            else
            {
                Play(Sfxs.char_mad_jump_climb_left);
                Dust.Burst(Center + Vector2.UnitX * -2, Calc.UpRight, 4);
            }
        }
```

## 外部弹跳接口 Bounce、SuperBounce、SideBounce、Rebound、ReflectBounce

Player.cs:1844-1989 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L1844-L1989) · ID: bounce-family

```csharp
        public void Bounce(float fromY)
        {
            if (StateMachine.State == StBoost && CurrentBooster != null)
            {
                CurrentBooster.PlayerReleased();
                CurrentBooster = null;
            }

            var was = Collider;
            Collider = normalHitbox;
            {
                MoveVExact((int)(fromY - Bottom));
                if (!Inventory.NoRefills)
                    RefillDash();
                RefillStamina();
                StateMachine.State = StNormal;

                jumpGraceTimer = 0;
                varJumpTimer = BounceVarJumpTime;
                AutoJump = true;
                AutoJumpTimer = BounceAutoJumpTime;
                dashAttackTimer = 0;
                wallSlideTimer = WallSlideTime;
                wallBoostTimer = 0;

                varJumpSpeed = Speed.Y = BounceSpeed;
                launched = false;

                Input.Rumble(RumbleStrength.Light, RumbleLength.Medium);
                Sprite.Scale = new Vector2(.6f, 1.4f);
            }
            Collider = was;
        }

        public void SuperBounce(float fromY)
        {
            if (StateMachine.State == StBoost && CurrentBooster != null)
            {
                CurrentBooster.PlayerReleased();
                CurrentBooster = null;
            }

            var was = Collider;
            Collider = normalHitbox;

            {
                MoveV(fromY - Bottom);
                if (!Inventory.NoRefills)
                    RefillDash();
                RefillStamina();
                StateMachine.State = StNormal;

                jumpGraceTimer = 0;
                varJumpTimer = SuperBounceVarJumpTime;
                AutoJump = true;
                AutoJumpTimer = 0;
                dashAttackTimer = 0;
                wallSlideTimer = WallSlideTime;
                wallBoostTimer = 0;

                Speed.X = 0;
                varJumpSpeed = Speed.Y = SuperBounceSpeed;
                launched = false;

                level.DirectionalShake(-Vector2.UnitY, .1f);
                Input.Rumble(RumbleStrength.Medium, RumbleLength.Medium);
                Sprite.Scale = new Vector2(.5f, 1.5f);
            }

            Collider = was;
        }

        private const float SideBounceSpeed = 240;
        private const float SideBounceForceMoveXTime = .3f;

        public void SideBounce(int dir, float fromX, float fromY)
        {
            var was = Collider;
            Collider = normalHitbox;
            {
                MoveV(Calc.Clamp(fromY - Bottom, -4, 4));
                if (dir > 0)
                    MoveH(fromX - Left);
                else if (dir < 0)
                    MoveH(fromX - Right);
                if (!Inventory.NoRefills)
                    RefillDash();
                RefillStamina();
                StateMachine.State = StNormal;

                jumpGraceTimer = 0;
                varJumpTimer = BounceVarJumpTime;
                AutoJump = true;
                AutoJumpTimer = 0;
                dashAttackTimer = 0;
                wallSlideTimer = WallSlideTime;
                forceMoveX = dir;
                forceMoveXTimer = SideBounceForceMoveXTime;
                wallBoostTimer = 0;
                launched = false;

                Speed.X = SideBounceSpeed * dir;
                varJumpSpeed = Speed.Y = BounceSpeed;

                level.DirectionalShake(Vector2.UnitX * dir, .1f);
                Input.Rumble(RumbleStrength.Medium, RumbleLength.Medium);
                Sprite.Scale = new Vector2(1.5f, .5f);
            }
            Collider = was;
        }

        public void Rebound(int direction = 0)
        {
            Speed.X = direction * ReboundSpeedX;
            Speed.Y = ReboundSpeedY;
            varJumpSpeed = Speed.Y;
            varJumpTimer = ReboundVarJumpTime;
            AutoJump = true;
            AutoJumpTimer = 0;
            dashAttackTimer = 0;
            wallSlideTimer = WallSlideTime;
            wallBoostTimer = 0;
            launched = false;

            dashAttackTimer = 0;
            forceMoveXTimer = 0;
            StateMachine.State = StNormal;
        }

        public void ReflectBounce(Vector2 direction)
        {
            if (direction.X != 0)
                Speed.X = direction.X * ReflectBoundSpeed;
            if (direction.Y != 0)
                Speed.Y = direction.Y * ReflectBoundSpeed;

            AutoJumpTimer = 0;
            dashAttackTimer = 0;
            wallSlideTimer = WallSlideTime;
            wallBoostTimer = 0;
            launched = false;

            dashAttackTimer = 0;
            forceMoveXTimer = 0;
            StateMachine.State = StNormal;
        }
```

## 最大冲刺数、冲刺补充和体力补充接口

Player.cs:1991-2028 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L1991-L2028) · ID: refill-api

```csharp
        public int MaxDashes
        {
            get
            {
                if (SaveData.Instance.AssistMode && SaveData.Instance.Assists.DashMode != Assists.DashModes.Normal && !level.InCutscene)
                    return 2;
                else
                    return Inventory.Dashes;
            }
        }

        public bool RefillDash()
        {
            if (Dashes < MaxDashes)
            {
                Dashes = MaxDashes;
                return true;
            }
            else
                return false;
        }

        public bool UseRefill()
        {
            if (Dashes < MaxDashes || Stamina < ClimbTiredThreshold)
            {
                Dashes = MaxDashes;
                RefillStamina();
                return true;
            }
            else
                return false;
        }

        public void RefillStamina()
        {
            Stamina = ClimbMaxStamina;
        }
```

## 移动平台速度继承与上限

Player.cs:2091-2107 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L2091-L2107) · ID: lift-boost

```csharp
        private Vector2 LiftBoost
        {
            get
            {
                Vector2 val = LiftSpeed;

                if (Math.Abs(val.X) > LiftXCap)
                    val.X = LiftXCap * Math.Sign(val.X);

                if (val.Y > 0)
                    val.Y = 0;
                else if (val.Y < LiftYCap)
                    val.Y = LiftYCap;

                return val;
            }
        }
```

## 蹲姿、起身空间、蹲姿碰撞箱检查

Player.cs:2113-2183 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L2113-L2183) · ID: ducking

```csharp
        public bool Ducking
        {
            get
            {
                return Collider == duckHitbox || Collider == duckHurtbox;
            }

            set
            {
                if (value)
                {
                    Collider = duckHitbox;
                    hurtbox = duckHurtbox;
                    
                }
                else
                {
                    Collider = normalHitbox;
                    hurtbox = normalHurtbox;
                }
            }
        }

        public bool CanUnDuck
        {
            get
            {
                if (!Ducking)
                    return true;

                Collider was = Collider;
                Collider = normalHitbox;
                bool ret = !CollideCheck<Solid>();
                Collider = was;
                return ret;
            }
        }

        public bool CanUnDuckAt(Vector2 at)
        {
            Vector2 was = Position;
            Position = at;
            bool ret = CanUnDuck;
            Position = was;
            return ret;
        }

        public bool DuckFreeAt(Vector2 at)
        {
            Vector2 oldP = Position;
            Collider oldC = Collider;
            Position = at;
            Collider = duckHitbox;

            bool ret = !CollideCheck<Solid>();

            Position = oldP;
            Collider = oldC;

            return ret;
        }

        private void Duck()
        {
            Collider = duckHitbox;
        }

        private void UnDuck()
        {
            Collider = normalHitbox;
        }
```

## 外部土狼时间和点弹跳接口

Player.cs:2261-2310 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L2261-L2310) · ID: grace-and-pointbounce

```csharp
        public void StartJumpGraceTime()
        {
            jumpGraceTimer = JumpGraceTime;
        }

        public override bool IsRiding(Solid solid)
        {
            if (StateMachine.State == StDreamDash)
                return CollideCheck(solid);

            if (StateMachine.State == StClimb || StateMachine.State == StHitSquash)
                return CollideCheck(solid, Position + Vector2.UnitX * (int)Facing);

            return base.IsRiding(solid);
        }

        public override bool IsRiding(JumpThru jumpThru)
        {
            if (StateMachine.State == StDreamDash)
                return false;

            return StateMachine.State != StClimb && Speed.Y >= 0 && base.IsRiding(jumpThru);
        }

        public bool BounceCheck(float y)
        {
            return Bottom <= y + 3;
        }

        public void PointBounce(Vector2 from)
        {
            const float BounceSpeed = 200f;
            const float MinX = 120f;

            if (StateMachine.State == StBoost && CurrentBooster != null)
                CurrentBooster.PlayerReleased();

            RefillDash();
            RefillStamina();
            Speed = (Center - from).SafeNormalize(BounceSpeed);
            Speed.X *= 1.2f;

            if (Math.Abs(Speed.X) < MinX)
            {
                if (Speed.X == 0)
                    Speed.X = -(int)Facing * MinX;
                else
                    Speed.X = Math.Sign(Speed.X) * MinX;
            }
        }
```

## 水平碰撞：冲刺方块、纠角、梦境入口、保速

Player.cs:2359-2451 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L2359-L2451) · ID: horizontal-collision

```csharp
        private void OnCollideH(CollisionData data)
        {
            if (StateMachine.State == StStarFly)
            {
                if (starFlyTimer < StarFlyEndNoBounceTime)
                    Speed.X = 0;
                else
                {
                    Play(Sfxs.game_06_feather_state_bump);
                    Input.Rumble(RumbleStrength.Light, RumbleLength.Medium);
                    Speed.X *= StarFlyWallBounce;
                }
                return;
            }

            if (StateMachine.State == StDreamDash)
                return;

            //Dash Blocks
            if (DashAttacking && data.Hit != null && data.Hit.OnDashCollide != null && data.Direction.X == Math.Sign(DashDir.X))
            {
                var result = data.Hit.OnDashCollide(this, data.Direction);
                if (StateMachine.State == StRedDash)
                    result = DashCollisionResults.Ignore;

                if (result == DashCollisionResults.Rebound)
                {
                    Rebound(-Math.Sign(Speed.X));
                    return;
                }
                else if (result == DashCollisionResults.Bounce)
                {
                    ReflectBounce(new Vector2(-Math.Sign(Speed.X), 0));
                    return;
                }
                else if (result == DashCollisionResults.Ignore)
                    return;
            }

            //Dash corner correction
            if (StateMachine.State == StDash || StateMachine.State == StRedDash)
            {
                if (onGround && DuckFreeAt(Position + Vector2.UnitX * Math.Sign(Speed.X)))
                {
                    Ducking = true;
                    return;
                }
                else if (Speed.Y == 0 && Speed.X != 0)
                {
                    for (int i = 1; i <= DashCornerCorrection; i++)
                    {
                        for (int j = 1; j >= -1; j -= 2)
                        {
                            if (!CollideCheck<Solid>(Position + new Vector2(Math.Sign(Speed.X), i * j)))
                            {
                                MoveVExact(i * j);
                                MoveHExact(Math.Sign(Speed.X));
                                return;
                            }
                        }
                    }
                }
            }

            //Dream Dash
            if (DreamDashCheck(Vector2.UnitX * Math.Sign(Speed.X)))
            {
                StateMachine.State = StDreamDash;
                dashAttackTimer = 0;
                return;
            }

            //Speed retention
            if (wallSpeedRetentionTimer <= 0)
            {
                wallSpeedRetained = Speed.X;
                wallSpeedRetentionTimer = WallSpeedRetentionTime;
            }

            //Collide event
            if (data.Hit != null && data.Hit.OnCollide != null)
                data.Hit.OnCollide(data.Direction);

            Speed.X = 0;
            dashAttackTimer = 0;

            if (StateMachine.State == StRedDash)
            {
                Input.Rumble(RumbleStrength.Medium, RumbleLength.Short);
                level.Displacement.AddBurst(Center, .5f, 8, 48, .4f, Ease.QuadOut, Ease.QuadOut);
                StateMachine.State = StHitSquash;
            }
        }
```

## 垂直碰撞：冲刺方块、纠角、斜下落地乘速、撞顶

Player.cs:2453-2640 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L2453-L2640) · ID: vertical-collision

```csharp
        private void OnCollideV(CollisionData data)
        {
            if (StateMachine.State == StStarFly)
            {
                if (starFlyTimer < StarFlyEndNoBounceTime)
                    Speed.Y = 0;
                else
                {
                    Play(Sfxs.game_06_feather_state_bump);
                    Input.Rumble(RumbleStrength.Light, RumbleLength.Medium);
                    Speed.Y *= StarFlyWallBounce;
                }
                return;
            }
            else if (StateMachine.State == StSwim)
            {
                Speed.Y = 0;
                return;
            }
            else if (StateMachine.State == StDreamDash)
                return;

            //Dash Blocks
            if (data.Hit != null && data.Hit.OnDashCollide != null)
            {
                if (DashAttacking && data.Direction.Y == Math.Sign(DashDir.Y))
                {
                    var result = data.Hit.OnDashCollide(this, data.Direction);
                    if (StateMachine.State == StRedDash)
                        result = DashCollisionResults.Ignore;

                    if (result == DashCollisionResults.Rebound)
                    {
                        Rebound(0);
                        return;
                    }
                    else if (result == DashCollisionResults.Bounce)
                    {
                        ReflectBounce(new Vector2(0, -Math.Sign(Speed.Y)));
                        return;
                    }
                    else if (result == DashCollisionResults.Ignore)
                        return;
                }
                else if (StateMachine.State == StSummitLaunch)
                {
                    data.Hit.OnDashCollide(this, data.Direction);
                    return;
                }
            }

            if (Speed.Y > 0)
            {
                //Dash corner correction
                if ((StateMachine.State == StDash || StateMachine.State == StRedDash) && !dashStartedOnGround)
                {
                    if (Speed.X <= 0)
                    {
                        for (int i = -1; i >= -DashCornerCorrection; i--)
                        {
                            if (!OnGround(Position + new Vector2(i, 0)))
                            {
                                MoveHExact(i);
                                MoveVExact(1);
                                return;
                            }
                        }
                    }

                    if (Speed.X >= 0)
                    {
                        for (int i = 1; i <= DashCornerCorrection; i++)
                        {
                            if (!OnGround(Position + new Vector2(i, 0)))
                            {
                                MoveHExact(i);
                                MoveVExact(1);
                                return;
                            }
                        }
                    }
                }

                //Dream Dash
                if (DreamDashCheck(Vector2.UnitY * Math.Sign(Speed.Y)))
                {
                    StateMachine.State = StDreamDash;
                    dashAttackTimer = 0;
                    return;
                }

                //Dash Slide
                if (DashDir.X != 0 && DashDir.Y > 0 && Speed.Y > 0)
                {
                    DashDir.X = Math.Sign(DashDir.X);
                    DashDir.Y = 0;
                    Speed.Y = 0;
                    Speed.X *= DodgeSlideSpeedMult;
                    Ducking = true;
                }

                //Landed on ground effects
                if (StateMachine.State != StClimb)
                {
                    float squish = Math.Min(Speed.Y / FastMaxFall, 1);
                    Sprite.Scale.X = MathHelper.Lerp(1, 1.6f, squish);
                    Sprite.Scale.Y = MathHelper.Lerp(1, .4f, squish);

                    if (Speed.Y >= MaxFall / 2)
                        Dust.Burst(Position, Calc.Angle(new Vector2(0, -1)), 8);

                    if (highestAirY < Y - 50 && Speed.Y >= MaxFall && Math.Abs(Speed.X) >= MaxRun)
                        Sprite.Play(PlayerSprite.RunStumble);

                    Input.Rumble(RumbleStrength.Light, RumbleLength.Short);

                    // landed SFX
                    var platform = SurfaceIndex.GetPlatformByPriority(CollideAll<Platform>(Position + new Vector2(0, 1), temp));
                    if (platform != null)
                    {
                        var surface = platform.GetLandSoundIndex(this);
                        if (surface >= 0)
                            Play(playFootstepOnLand > 0f ? Sfxs.char_mad_footstep : Sfxs.char_mad_land, SurfaceIndex.Param, surface);
                        if (platform is DreamBlock)
                            (platform as DreamBlock).FootstepRipple(Position);
                    }

                    playFootstepOnLand = 0f;
                }
            }
            else 
            {
                if (Speed.Y < 0)
                {
                    //Corner Correction
                    {
                        if (Speed.X <= 0)
                        {
                            for (int i = 1; i <= UpwardCornerCorrection; i++)
                            {
                                if (!CollideCheck<Solid>(Position + new Vector2(-i, -1)))
                                {
                                    Position += new Vector2(-i, -1);
                                    return;
                                }
                            }
                        }

                        if (Speed.X >= 0)
                        {
                            for (int i = 1; i <= UpwardCornerCorrection; i++)
                            {
                                if (!CollideCheck<Solid>(Position + new Vector2(i, -1)))
                                {
                                    Position += new Vector2(i, -1);
                                    return;
                                }
                            }
                        }
                    }

                    if (varJumpTimer < VarJumpTime - CeilingVarJumpGrace)
                        varJumpTimer = 0;
                }

                //Dream Dash
                if (DreamDashCheck(Vector2.UnitY * Math.Sign(Speed.Y)))
                {
                    StateMachine.State = StDreamDash;
                    dashAttackTimer = 0;
                    return;
                }
            }

            //Collide event
            if (data.Hit != null && data.Hit.OnCollide != null)
                data.Hit.OnCollide(data.Direction);

            dashAttackTimer = 0;
            Speed.Y = 0;

            if (StateMachine.State == StRedDash)
            {
                Input.Rumble(RumbleStrength.Medium, RumbleLength.Short);
                level.Displacement.AddBurst(Center, .5f, 8, 48, .4f, Ease.QuadOut, Ease.QuadOut);
                StateMachine.State = StHitSquash;
            }
        }
```

## 梦境冲刺入口与边缘纠正

Player.cs:2642-2705 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L2642-L2705) · ID: dream-entry

```csharp
        private bool DreamDashCheck(Vector2 dir)
        {
            if (Inventory.DreamDash && DashAttacking && (dir.X == Math.Sign(DashDir.X) || dir.Y == Math.Sign(DashDir.Y)))
            {
                var block = CollideFirst<DreamBlock>(Position + dir);

                if (block != null)
                {
                    if (CollideCheck<Solid, DreamBlock>(Position + dir))
                    {
                        Vector2 side = new Vector2(Math.Abs(dir.Y), Math.Abs(dir.X));

                        bool checkNegative, checkPositive;
                        if (dir.X != 0)
                        {
                            checkNegative = Speed.Y <= 0;
                            checkPositive = Speed.Y >= 0;
                        }
                        else
                        {
                            checkNegative = Speed.X <= 0;
                            checkPositive = Speed.X >= 0;
                        }

                        if (checkNegative)
                        {
                            for (int i = -1; i >= -DashCornerCorrection; i--)
                            {
                                var at = Position + dir + side * i;
                                if (!CollideCheck<Solid, DreamBlock>(at))
                                {
                                    Position += side * i;
                                    dreamBlock = block;
                                    return true;
                                }
                            }
                        }

                        if (checkPositive)
                        {
                            for (int i = 1; i <= DashCornerCorrection; i++)
                            {
                                var at = Position + dir + side * i;
                                if (!CollideCheck<Solid, DreamBlock>(at))
                                {
                                    Position += side * i;
                                    dreamBlock = block;
                                    return true;
                                }
                            }
                        }

                        return false;
                    }
                    else
                    {
                        dreamBlock = block;
                        return true;
                    }
                }        
            }

            return false;
        }
```

## 普通状态进入、离开和爬墙检查

Player.cs:2760-2788 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L2760-L2788) · ID: normal-state-entry

```csharp
        private void NormalBegin()
        {
            maxFall = MaxFall;
        }

        private void NormalEnd()
        {
            wallBoostTimer = 0;
            wallSpeedRetentionTimer = 0;
            hopWaitX = 0;
        }

        public bool ClimbBoundsCheck(int dir)
        {
            return Left + dir * ClimbCheckDist >= level.Bounds.Left && Right + dir * ClimbCheckDist < level.Bounds.Right;
        }

        public bool ClimbCheck(int dir, int yAdd = 0)
        {
            return ClimbBoundsCheck(dir) && !ClimbBlocker.Check(Scene, this, Position + Vector2.UnitY * yAdd + Vector2.UnitX * ClimbCheckDist * (int)Facing) && CollideCheck<Solid>(Position + new Vector2(dir * ClimbCheckDist, yAdd));
        }

        private const float SpacePhysicsMult = .6f;

        private int NormalUpdate()
        {
            //Use Lift Boost if walked off platform
            if (LiftBoost.Y < 0 && wasOnGround && !onGround && Speed.Y >= 0)
                Speed.Y = LiftBoost.Y;
```

## 普通状态触发冲刺、蹲姿和高速摩擦

Player.cs:2823-2895 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L2823-L2895) · ID: normal-dash-duck

```csharp
                //Dashing
                if (CanDash)
                {
                    Speed += LiftBoost;                   
                    return StartDash();
                }

                //Ducking
                if (Ducking)
                {
                    if (onGround && Input.MoveY != 1)
                    {
                        if (CanUnDuck)
                        {
                            Ducking = false;
                            Sprite.Scale = new Vector2(.8f, 1.2f);
                        }
                        else if (Speed.X == 0)
                        {
                            for (int i = DuckCorrectCheck; i > 0; i--)
                            {
                                if (CanUnDuckAt(Position + Vector2.UnitX * i))
                                {
                                    MoveH(DuckCorrectSlide * Engine.DeltaTime);
                                    break;
                                }
                                else if (CanUnDuckAt(Position - Vector2.UnitX * i))
                                {
                                    MoveH(-DuckCorrectSlide * Engine.DeltaTime);
                                    break;
                                }
                            }
                        }
                    }
                }
                else if(onGround && Input.MoveY == 1 && Speed.Y >= 0)
                {
                    Ducking = true;
                    Sprite.Scale = new Vector2(1.4f, .6f);
                }
            }
            else
            {
                //Throw
                if (!Input.Grab.Check && minHoldTimer <= 0)
                    Throw();

                //Ducking
                if (!Ducking && onGround && Input.MoveY == 1 && Speed.Y >= 0)
                {
                    Drop();
                    Ducking = true;
                    Sprite.Scale = new Vector2(1.4f, .6f);
                }
            }

            //Running and Friction
            if (Ducking && onGround)
                Speed.X = Calc.Approach(Speed.X, 0, DuckFriction * Engine.DeltaTime);
            else
            {
                float mult = onGround ? 1 : AirMult;
                if (onGround && level.CoreMode == Session.CoreModes.Cold)
                    mult *= .3f;

                float max = Holding == null ? MaxRun : HoldingMaxRun;
                if (level.InSpace)
                    max *= SpacePhysicsMult;
                if (Math.Abs(Speed.X) > max && Math.Sign(Speed.X) == moveX)
                    Speed.X = Calc.Approach(Speed.X, max * moveX, RunReduce * mult * Engine.DeltaTime);  //Reduce back from beyond the max speed
                else
                    Speed.X = Calc.Approach(Speed.X, max * moveX, RunAccel * mult * Engine.DeltaTime);   //Approach the max speed
            }
```

## 快落、重力、变高跳、普通跳和后续蹬墙跳

Player.cs:2897-3007 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L2897-L3007) · ID: normal-vertical-jump

```csharp
            //Vertical
            {
                //Calculate current max fall speed
                {
                    float mf = MaxFall;
                    float fmf = FastMaxFall;

                    if (level.InSpace)
                    {
                        mf *= SpacePhysicsMult;
                        fmf *= SpacePhysicsMult;
                    }

                    //Fast Fall
                    if (Input.MoveY == 1 && Speed.Y >= mf)
                    {
                        maxFall = Calc.Approach(maxFall, fmf, FastMaxAccel * Engine.DeltaTime);

                        float half = mf + (fmf - mf) * .5f;
                        if (Speed.Y >= half)
                        {
                            float spriteLerp = Math.Min(1f, (Speed.Y - half) / (fmf - half));
                            Sprite.Scale.X = MathHelper.Lerp(1f, .5f, spriteLerp);
                            Sprite.Scale.Y = MathHelper.Lerp(1f, 1.5f, spriteLerp);
                        }
                    }
                    else
                        maxFall = Calc.Approach(maxFall, mf, FastMaxAccel * Engine.DeltaTime);
                }

                //Gravity
                if (!onGround)
                {
                    float max = maxFall;

                    //Wall Slide
                    if ((moveX == (int)Facing || (moveX == 0 && Input.Grab.Check)) && Input.MoveY.Value != 1)
                    {
                        if (Speed.Y >= 0 && wallSlideTimer > 0 && Holding == null && ClimbBoundsCheck((int)Facing) && CollideCheck<Solid>(Position + Vector2.UnitX * (int)Facing) && CanUnDuck)
                        {
                            Ducking = false;
                            wallSlideDir = (int)Facing;
                        }

                        if (wallSlideDir != 0)
                        {
                            if (wallSlideTimer > WallSlideTime * .5f && ClimbBlocker.Check(level, this, Position + Vector2.UnitX * wallSlideDir))
                                wallSlideTimer = WallSlideTime * .5f;

                            max = MathHelper.Lerp(MaxFall, WallSlideStartMax, wallSlideTimer / WallSlideTime);
                            if (wallSlideTimer / WallSlideTime > .65f)
                                CreateWallSlideParticles(wallSlideDir);
                        }
                    }

                    float mult = (Math.Abs(Speed.Y) < HalfGravThreshold && (Input.Jump.Check || AutoJump)) ? .5f : 1f;

                    if (level.InSpace)
                        mult *= SpacePhysicsMult;

                    Speed.Y = Calc.Approach(Speed.Y, max, Gravity * mult * Engine.DeltaTime);
                }

                //Variable Jumping
                if (varJumpTimer > 0)
                {
                    if (AutoJump || Input.Jump.Check)
                        Speed.Y = Math.Min(Speed.Y, varJumpSpeed);
                    else
                        varJumpTimer = 0;
                }

                //Jumping
                if (Input.Jump.Pressed)
                {
                    Water water = null;
                    if (jumpGraceTimer > 0)
                    {
                        Jump();
                    }
                    else if (CanUnDuck)
                    {
                        bool canUnduck = CanUnDuck;
                        if (canUnduck && WallJumpCheck(1))
                        {
                            if (Facing == Facings.Right && Input.Grab.Check && Stamina > 0 && Holding == null && !ClimbBlocker.Check(Scene, this, Position + Vector2.UnitX * WallJumpCheckDist))
                                ClimbJump();
                            else if (DashAttacking && DashDir.X == 0 && DashDir.Y == -1)
                                SuperWallJump(-1);
                            else
                                WallJump(-1);
                        }
                        else if (canUnduck && WallJumpCheck(-1))
                        {
                            if (Facing == Facings.Left && Input.Grab.Check && Stamina > 0 && Holding == null && !ClimbBlocker.Check(Scene, this, Position + Vector2.UnitX * -WallJumpCheckDist))
                                ClimbJump();
                            else if (DashAttacking && DashDir.X == 0 && DashDir.Y == -1)
                                SuperWallJump(1);
                            else
                                WallJump(1);
                        }
                        else if ((water = CollideFirst<Water>(Position + Vector2.UnitY * 2)) != null)
                        {
                            Jump();
                            water.TopSurface.DoRipple(Position, 1);
                        }
                    }
                }
            }

            return StNormal;
```

## 爬墙状态中跳跃和冲刺

Player.cs:3102-3126 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L3102-L3126) · ID: climb-dash

```csharp
        private int ClimbUpdate()
        {
            climbNoMoveTimer -= Engine.DeltaTime;
            
            //Refill stamina on ground
            if (onGround)
                Stamina = ClimbMaxStamina;

            //Wall Jump
            if (Input.Jump.Pressed && (!Ducking || CanUnDuck))
            {
                if (moveX == -(int)Facing)
                    WallJump(-(int)Facing);
                else
                    ClimbJump();

                return StNormal;
            }

            //Dashing
            if (CanDash)
            {
                Speed += LiftBoost;
                return StartDash();
            }
```

## 完整普通冲刺状态：消耗、条件、开始、取消和协程

Player.cs:3356-3636 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L3356-L3636) · ID: dash-state

```csharp
        #region Dash State

        public int StartDash()
        {
            wasDashB = Dashes == 2;
            Dashes = Math.Max(0, Dashes - 1);
            Input.Dash.ConsumeBuffer();
            return StDash;
        }

        public bool DashAttacking
        {
            get
            {
                return dashAttackTimer > 0 || StateMachine.State == StRedDash;
            }
        }

        private Vector2 beforeDashSpeed;
        private bool wasDashB;

        public bool CanDash
        {
            get
            {
                return Input.Dash.Pressed && dashCooldownTimer <= 0 && Dashes > 0 &&
                    (TalkComponent.PlayerOver == null || !Input.Talk.Pressed);
            }
        }

        public bool StartedDashing
        {
            get; private set;
        }

        private void CallDashEvents()
        {
            if (!calledDashEvents)
            {
                calledDashEvents = true;
                if (CurrentBooster == null)
                {
                    // Increment Counter
                    SaveData.Instance.TotalDashes++;
                    level.Session.Dashes++;
                    Stats.Increment(Stat.DASHES);

                    // Sfxs
                    {
                        var rightDashSound = DashDir.Y < 0 || (DashDir.Y == 0 && DashDir.X > 0);
                        if (DashDir == Vector2.Zero)
                            rightDashSound = Facing == Facings.Right;

                        if (rightDashSound)
                        {
                            if (wasDashB)
                                Play(Sfxs.char_mad_dash_pink_right);
                            else
                                Play(Sfxs.char_mad_dash_red_right);
                        }
                        else
                        {
                            if (wasDashB)
                                Play(Sfxs.char_mad_dash_pink_left);
                            else
                                Play(Sfxs.char_mad_dash_red_left);
                        }

                        if (SwimCheck())
                            Play(Sfxs.char_mad_water_dash_gen);
                    }

                    //Dash Listeners
                    foreach (DashListener dl in Scene.Tracker.GetComponents<DashListener>())
                        if (dl.OnDash != null)
                            dl.OnDash(DashDir);
                }
                else
                {
                    //Booster
                    CurrentBooster.PlayerBoosted(this, DashDir);
                    CurrentBooster = null;
                }
            }
        }

        private void DashBegin()
        {
            calledDashEvents = false;
            dashStartedOnGround = onGround;
            launched = false;

            if (Engine.TimeRate > 0.25f)
                Celeste.Freeze(.05f);
            dashCooldownTimer = DashCooldown;
            dashRefillCooldownTimer = DashRefillCooldown;
            StartedDashing = true;
            wallSlideTimer = WallSlideTime;
            dashTrailTimer = 0;

            level.Displacement.AddBurst(Center, .4f, 8, 64, .5f, Ease.QuadOut, Ease.QuadOut);

            Input.Rumble(RumbleStrength.Strong, RumbleLength.Medium);

            dashAttackTimer = DashAttackTime;
            beforeDashSpeed = Speed;
            Speed = Vector2.Zero;
            DashDir = Vector2.Zero;

            if (!onGround && Ducking && CanUnDuck)
                Ducking = false;
        }

        private void DashEnd()
        {
            CallDashEvents();
        }

        private int DashUpdate()
        {
            StartedDashing = false;

            //Trail
            if (dashTrailTimer > 0)
            {
                dashTrailTimer -= Engine.DeltaTime;
                if (dashTrailTimer <= 0)
                    CreateTrail();
            }

            //Grab Holdables
            if (Holding == null && Input.Grab.Check && !IsTired && CanUnDuck)
            {
                //Grabbing Holdables
                foreach (Holdable hold in Scene.Tracker.GetComponents<Holdable>())
                    if (hold.Check(this) && Pickup(hold))
                        return StPickup;
            }

            if (DashDir.Y == 0)
            {
                //JumpThru Correction
                foreach (JumpThru jt in Scene.Tracker.GetEntities<JumpThru>())
                    if (CollideCheck(jt) && Bottom - jt.Top <= DashHJumpThruNudge)
                        MoveVExact((int)(jt.Top - Bottom));

                //Super Jump
                if (CanUnDuck && Input.Jump.Pressed && jumpGraceTimer > 0)
                {
                    SuperJump();
                    return StNormal;
                }
            }

            if (DashDir.X == 0 && DashDir.Y == -1)
            {
                if (Input.Jump.Pressed && CanUnDuck)
                {
                    if (WallJumpCheck(1))
                    {
                        SuperWallJump(-1);
                        return StNormal;
                    }
                    else if (WallJumpCheck(-1))
                    {
                        SuperWallJump(1);
                        return StNormal;
                    }
                }
            }
            else
            {
                if (Input.Jump.Pressed && CanUnDuck)
                {
                    if (WallJumpCheck(1))
                    {
                        WallJump(-1);
                        return StNormal;
                    }
                    else if (WallJumpCheck(-1))
                    {
                        WallJump(1);
                        return StNormal;
                    }
                }
            }

            if (Speed != Vector2.Zero && level.OnInterval(0.02f))
                level.ParticlesFG.Emit(wasDashB ? P_DashB : P_DashA, Center + Calc.Random.Range(Vector2.One *-2, Vector2.One * 2), DashDir.Angle());
            return StDash;
        }

        private IEnumerator DashCoroutine()
        {
            yield return null;

            var dir = lastAim;
            if (OverrideDashDirection.HasValue)
                dir = OverrideDashDirection.Value;

            var newSpeed = dir * DashSpeed;
            if (Math.Sign(beforeDashSpeed.X) == Math.Sign(newSpeed.X) && Math.Abs(beforeDashSpeed.X) > Math.Abs(newSpeed.X))
                newSpeed.X = beforeDashSpeed.X;
            Speed = newSpeed;

            if (CollideCheck<Water>())
                Speed *= SwimDashSpeedMult;

            DashDir = dir;
            SceneAs<Level>().DirectionalShake(DashDir, .2f);

            if (DashDir.X != 0)
                Facing = (Facings)Math.Sign(DashDir.X);

            CallDashEvents();

            //Feather particles
            if (StateMachine.PreviousState == StStarFly)
                level.Particles.Emit(FlyFeather.P_Boost, 12, Center, Vector2.One * 4, (-dir).Angle());

            //Dash Slide
            if (onGround && DashDir.X != 0 && DashDir.Y > 0 && Speed.Y > 0 
                && (!Inventory.DreamDash || !CollideCheck<DreamBlock>(Position + Vector2.UnitY)))
            {
                DashDir.X = Math.Sign(DashDir.X);
                DashDir.Y = 0;
                Speed.Y = 0;
                Speed.X *= DodgeSlideSpeedMult;
                Ducking = true;
            }
            
            SlashFx.Burst(Center, DashDir.Angle());
            CreateTrail();
            dashTrailTimer = .08f;

            //Swap Block check
            if (DashDir.X != 0 && Input.Grab.Check)
            {
                var swapBlock = CollideFirst<SwapBlock>(Position + Vector2.UnitX * Math.Sign(DashDir.X));
                if (swapBlock != null && swapBlock.Direction.X == Math.Sign(DashDir.X))
                {
                    StateMachine.State = StClimb;
                    Speed = Vector2.Zero;
                    yield break;
                }
            }

            //Stick to Swap Blocks
            Vector2 swapCancel = Vector2.One;
            foreach (SwapBlock swapBlock in Scene.Tracker.GetEntities<SwapBlock>())
            {
                if (CollideCheck(swapBlock, Position + Vector2.UnitY))
                {
                    if (swapBlock != null && swapBlock.Swapping)
                    {
                        if (DashDir.X != 0 && swapBlock.Direction.X == Math.Sign(DashDir.X))
                            Speed.X = swapCancel.X = 0;
                        if (DashDir.Y != 0 && swapBlock.Direction.Y == Math.Sign(DashDir.Y))
                            Speed.Y = swapCancel.Y = 0;
                    }
                }
            }

            yield return DashTime;

            CreateTrail();

            AutoJump = true;
            AutoJumpTimer = 0;
            if (DashDir.Y <= 0)
            {
                Speed = DashDir * EndDashSpeed;
                Speed.X *= swapCancel.X;
                Speed.Y *= swapCancel.Y;
            }
            if (Speed.Y < 0)
                Speed.Y *= EndDashUpMult;
            StateMachine.State = StNormal;
        }

        #endregion
```

## 水中冲刺倍率

Player.cs:3640-3647 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L3640-L3647) · ID: swim-dash-multiplier

```csharp
        private const float SwimYSpeedMult = .5f;    
        private const float SwimMaxRise = -60;
        private const float SwimVDeccel = 600f;
        private const float SwimMax = 80f;
        private const float SwimUnderwaterMax = 60f;
        private const float SwimAccel = 600f;
        private const float SwimReduce = 400f;
        private const float SwimDashSpeedMult = .75f;
```

## 水中冲刺入口：没有调用扣次数的 StartDash

Player.cs:3684-3699 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L3684-L3699) · ID: swim-dash

```csharp
        private int SwimUpdate()
        {
            if (!SwimCheck())
                return StNormal;

            //Never duck if possible
            if (CanUnDuck)
                Ducking = false;

            //Dashing
            if (CanDash)
            {
                //Dashes = Math.Max(0, Dashes - 1);
                Input.Dash.ConsumeBuffer();
                return StDash;
            }
```

## 气泡恢复与普通/红色气泡冲刺选择

Player.cs:3788-3828 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L3788-L3828) · ID: booster-dash

```csharp
        private void BoostBegin()
        {
            RefillDash();
            RefillStamina();
        }

        private void BoostEnd()
        {
            var to = (boostTarget - Collider.Center).Floor();
            MoveToX(to.X);
            MoveToY(to.Y);
        }

        private int BoostUpdate()
        {
            Vector2 targetAdd = Input.Aim.Value * 3;
            var to = Calc.Approach(ExactPosition, boostTarget - Collider.Center + targetAdd, BoostMoveSpeed * Engine.DeltaTime);
            MoveToX(to.X);
            MoveToY(to.Y);

            if (Input.Dash.Pressed)
            {
                Input.Dash.ConsumePress();
                if (boostRed)
                    return StRedDash;
                else
                    return StDash;
            }

            return StBoost;
        }

        private IEnumerator BoostCoroutine()
        {
            yield return BoostTime;

            if (boostRed)
                StateMachine.State = StRedDash;
            else
                StateMachine.State = StDash;
        }
```

## 完整红色气泡冲刺状态

Player.cs:3832-3929 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L3832-L3929) · ID: red-dash

```csharp
        #region Red Dash State

        private void RedDashBegin()
        {
            calledDashEvents = false;
            dashStartedOnGround = false;

            Celeste.Freeze(.05f);
            Dust.Burst(Position, Calc.Angle(-DashDir), 8);
            dashCooldownTimer = DashCooldown;
            dashRefillCooldownTimer = DashRefillCooldown;
            StartedDashing = true;

            level.Displacement.AddBurst(Center, .5f, 0, 80, .666f, Ease.QuadOut, Ease.QuadOut);
            
            Input.Rumble(RumbleStrength.Strong, RumbleLength.Medium);

            dashAttackTimer = DashAttackTime;
            Speed = Vector2.Zero;

            if (!onGround && Ducking && CanUnDuck)
                Ducking = false;
        }

        private void RedDashEnd()
        {
            CallDashEvents();
        }

        private int RedDashUpdate()
        {
            StartedDashing = false;

            if (CanDash)
                return StartDash();

            if (DashDir.Y == 0)
            {
                //JumpThru Correction
                foreach (JumpThru jt in Scene.Tracker.GetEntities<JumpThru>())
                    if (CollideCheck(jt) && Bottom - jt.Top <= DashHJumpThruNudge)
                        MoveVExact((int)(jt.Top - Bottom));

                //Super Jump
                if (CanUnDuck && Input.Jump.Pressed && jumpGraceTimer > 0)
                {
                    SuperJump();
                    return StNormal;
                }
            }
            else if (DashDir.X == 0 && DashDir.Y == -1)
            {
                if (Input.Jump.Pressed && CanUnDuck)
                {
                    if (WallJumpCheck(1))
                    {
                        SuperWallJump(-1);
                        return StNormal;
                    }
                    else if (WallJumpCheck(-1))
                    {
                        SuperWallJump(1);
                        return StNormal;
                    }
                }
            }
            else
            {
                if (Input.Jump.Pressed && CanUnDuck)
                {
                    if (WallJumpCheck(1))
                    {
                        WallJump(-1);
                        return StNormal;
                    }
                    else if (WallJumpCheck(-1))
                    {
                        WallJump(1);
                        return StNormal;
                    }
                }
            }

            return StRedDash;
        }

        private IEnumerator RedDashCoroutine()
        {
            yield return null;

            Speed = lastAim * DashSpeed;
            DashDir = lastAim;
            SceneAs<Level>().DirectionalShake(DashDir, .2f);

            CallDashEvents();
        }

        #endregion
```

## 完整梦境冲刺与出块跳跃逻辑

Player.cs:4160-4307 · [固定版本原文](https://github.com/NoelFB/Celeste/blob/1b0ce45c75e05649ae91b44a8bb6b196684e4352/Source/Player/Player.cs#L4160-L4307) · ID: dream-dash

```csharp
        #region Dream Dash State

        private DreamBlock dreamBlock;
        private SoundSource dreamSfxLoop;
        private bool dreamJump;

        private void DreamDashBegin()
        {
            if (dreamSfxLoop == null)
                Add(dreamSfxLoop = new SoundSource());

            Speed = DashDir * DreamDashSpeed;
            TreatNaive = true;
            Depth = Depths.PlayerDreamDashing;
            dreamDashCanEndTimer = DreamDashMinTime;
            Stamina = ClimbMaxStamina;
            dreamJump = false;

            Play(Sfxs.char_mad_dreamblock_enter);
            Loop(dreamSfxLoop, Sfxs.char_mad_dreamblock_travel);
        }

        private void DreamDashEnd()
        {
            Depth = Depths.Player;
            if (!dreamJump)
            {
                AutoJump = true;
                AutoJumpTimer = 0;
            }
            if (!Inventory.NoRefills)
                RefillDash();
            RefillStamina();
            TreatNaive = false;

            if (dreamBlock != null)
            {
                if (DashDir.X != 0)
                {
                    jumpGraceTimer = JumpGraceTime;
                    dreamJump = true;
                }
                else
                    jumpGraceTimer = 0;

                dreamBlock.OnPlayerExit(this);
                dreamBlock = null;
            }

            Stop(dreamSfxLoop);
            Play(Sfxs.char_mad_dreamblock_exit);
            Input.Rumble(RumbleStrength.Medium, RumbleLength.Short);
        }

        private int DreamDashUpdate()
        {
            Input.Rumble(RumbleStrength.Light, RumbleLength.Medium);

            var oldPos = Position;
            NaiveMove(Speed * Engine.DeltaTime);
            if (dreamDashCanEndTimer > 0)
                dreamDashCanEndTimer -= Engine.DeltaTime;

            var block = CollideFirst<DreamBlock>();          
            if (block == null)
            {
                if (DreamDashedIntoSolid())
                {
                    if (SaveData.Instance.AssistMode && SaveData.Instance.Assists.Invincible)
                    {
                        Position = oldPos;
                        Speed *= -1;
                        Play(Sfxs.game_assist_dreamblockbounce);
                    }
                    else
                        Die(Vector2.Zero);
                }
                else if (dreamDashCanEndTimer <= 0)
                {
                    Celeste.Freeze(.05f);

                    if (Input.Jump.Pressed && DashDir.X != 0)
                    {
                        dreamJump = true;
                        Jump();
                    }
                    else
                    {
                        bool left = ClimbCheck(-1);
                        bool right = ClimbCheck(1);

                        if (Input.Grab.Check && (DashDir.Y >= 0 || DashDir.X != 0) && ((moveX == 1 && right) || (moveX == -1 && left)))
                        {
                            Facing = (Facings)moveX;
                            return StClimb;
                        }
                    }

                    return StNormal;
                }
            }
            else
            {
                dreamBlock = block;

                if (Scene.OnInterval(0.1f))
                    CreateTrail();

                //Displacement effect
                if (level.OnInterval(0.04f))
                {
                    var displacement = level.Displacement.AddBurst(Center, .3f, 0f, 40f);
                    displacement.WorldClipCollider = dreamBlock.Collider;
                    displacement.WorldClipPadding = 2;
                }
            }

            return StDreamDash;
        }

        private bool DreamDashedIntoSolid()
        {
            if (CollideCheck<Solid>())
            {
                for (int x = 1; x <= DreamDashEndWiggle; x++)
                {
                    for (int xm = -1; xm <= 1; xm += 2)
                    {
                        for (int y = 1; y <= DreamDashEndWiggle; y++)
                        {
                            for (int ym = -1; ym <= 1; ym += 2)
                            {
                                Vector2 add = new Vector2(x * xm, y * ym);
                                if (!CollideCheck<Solid>(Position + add))
                                {
                                    Position += add;
                                    return false;
                                }
                            }
                        }
                    }
                }

                return true;
            }
            else
                return false;
        }
```

