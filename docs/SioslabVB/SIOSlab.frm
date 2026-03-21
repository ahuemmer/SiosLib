VERSION 5.00
Begin VB.Form SIOSLAB 
   Appearance      =   0  '2D
   Caption         =   "SIOSLAB"
   ClientHeight    =   7260
   ClientLeft      =   1590
   ClientTop       =   1500
   ClientWidth     =   11805
   BeginProperty Font 
      Name            =   "MS Sans Serif"
      Size            =   8.25
      Charset         =   0
      Weight          =   700
      Underline       =   0   'False
      Italic          =   0   'False
      Strikethrough   =   0   'False
   EndProperty
   ForeColor       =   &H80000008&
   LinkTopic       =   "Form1"
   PaletteMode     =   1  'ZReihenfolge
   Picture         =   "SIOSlab.frx":0000
   ScaleHeight     =   484
   ScaleMode       =   3  'Pixel
   ScaleWidth      =   787
   Begin VB.TextBox Text13 
      Height          =   375
      Left            =   6000
      TabIndex        =   76
      Text            =   "10"
      Top             =   6720
      Width           =   735
   End
   Begin VB.OptionButton Option5 
      Caption         =   "COMx"
      Height          =   255
      Left            =   5040
      TabIndex        =   75
      Top             =   6720
      Width           =   855
   End
   Begin VB.Timer Timer2 
      Enabled         =   0   'False
      Interval        =   500
      Left            =   10200
      Top             =   6720
   End
   Begin VB.CommandButton Command2 
      Caption         =   "INIT CompuLAB"
      Height          =   375
      Left            =   7320
      TabIndex        =   72
      Top             =   5640
      Width           =   3255
   End
   Begin VB.Frame Frame4 
      Caption         =   "Compulab Outputs"
      Height          =   1455
      Left            =   6840
      TabIndex        =   61
      Top             =   3120
      Width           =   4575
      Begin VB.CheckBox Check32 
         Caption         =   "Check2"
         Height          =   255
         Left            =   840
         TabIndex        =   70
         Top             =   480
         Width           =   255
      End
      Begin VB.CheckBox Check31 
         Caption         =   "Check2"
         Height          =   255
         Left            =   1320
         TabIndex        =   69
         Top             =   480
         Width           =   255
      End
      Begin VB.CheckBox Check30 
         Caption         =   "Check2"
         Height          =   255
         Left            =   1800
         TabIndex        =   68
         Top             =   480
         Width           =   255
      End
      Begin VB.CheckBox Check29 
         Caption         =   "Check2"
         Height          =   255
         Left            =   2280
         TabIndex        =   67
         Top             =   480
         Width           =   255
      End
      Begin VB.CheckBox Check28 
         Caption         =   "Check2"
         Height          =   255
         Left            =   2760
         TabIndex        =   66
         Top             =   480
         Width           =   255
      End
      Begin VB.CheckBox Check27 
         Caption         =   "Check2"
         Height          =   255
         Left            =   3120
         TabIndex        =   65
         Top             =   480
         Width           =   255
      End
      Begin VB.CheckBox Check26 
         Caption         =   "Check2"
         Height          =   255
         Left            =   3480
         TabIndex        =   64
         Top             =   480
         Width           =   255
      End
      Begin VB.CheckBox Check25 
         Caption         =   "Check1"
         Height          =   255
         Left            =   3960
         TabIndex        =   63
         Top             =   480
         Width           =   255
      End
      Begin VB.TextBox Text9 
         BeginProperty Font 
            Name            =   "MS Sans Serif"
            Size            =   13.5
            Charset         =   0
            Weight          =   700
            Underline       =   0   'False
            Italic          =   0   'False
            Strikethrough   =   0   'False
         EndProperty
         Height          =   435
         Left            =   1560
         TabIndex        =   62
         Text            =   "0"
         Top             =   840
         Width           =   1095
      End
      Begin VB.Label Label14 
         Caption         =   "Dout"
         Height          =   255
         Left            =   120
         TabIndex        =   71
         Top             =   480
         Width           =   495
      End
   End
   Begin VB.Frame Frame3 
      Caption         =   "Compulab Inputs"
      Height          =   2775
      Left            =   6840
      TabIndex        =   45
      Top             =   120
      Width           =   4575
      Begin VB.TextBox Text10 
         BeginProperty Font 
            Name            =   "MS Sans Serif"
            Size            =   13.5
            Charset         =   0
            Weight          =   700
            Underline       =   0   'False
            Italic          =   0   'False
            Strikethrough   =   0   'False
         EndProperty
         Height          =   435
         Left            =   1560
         TabIndex        =   74
         Text            =   "0"
         Top             =   2160
         Width           =   1095
      End
      Begin VB.TextBox Text12 
         BeginProperty Font 
            Name            =   "MS Sans Serif"
            Size            =   13.5
            Charset         =   0
            Weight          =   700
            Underline       =   0   'False
            Italic          =   0   'False
            Strikethrough   =   0   'False
         EndProperty
         Height          =   435
         Left            =   2640
         TabIndex        =   57
         Top             =   960
         Width           =   1335
      End
      Begin VB.PictureBox Picture8 
         Height          =   255
         Left            =   720
         ScaleHeight     =   195
         ScaleWidth      =   1155
         TabIndex        =   56
         Top             =   1080
         Width           =   1215
         Begin VB.Shape Shape8 
            FillStyle       =   0  'Ausgefüllt
            Height          =   255
            Left            =   0
            Top             =   0
            Width           =   75
         End
      End
      Begin VB.TextBox Text11 
         BeginProperty Font 
            Name            =   "MS Sans Serif"
            Size            =   13.5
            Charset         =   0
            Weight          =   700
            Underline       =   0   'False
            Italic          =   0   'False
            Strikethrough   =   0   'False
         EndProperty
         Height          =   435
         Left            =   2640
         TabIndex        =   55
         Top             =   360
         Width           =   1335
      End
      Begin VB.PictureBox Picture7 
         Height          =   255
         Left            =   720
         ScaleHeight     =   195
         ScaleWidth      =   1155
         TabIndex        =   54
         Top             =   480
         Width           =   1215
         Begin VB.Shape Shape7 
            FillStyle       =   0  'Ausgefüllt
            Height          =   255
            Left            =   0
            Top             =   0
            Width           =   75
         End
      End
      Begin VB.CheckBox Check24 
         Caption         =   "Check1"
         Height          =   255
         Left            =   720
         TabIndex        =   53
         Top             =   1800
         Width           =   255
      End
      Begin VB.CheckBox Check23 
         Caption         =   "Check2"
         Height          =   255
         Left            =   1080
         TabIndex        =   52
         Top             =   1800
         Width           =   255
      End
      Begin VB.CheckBox Check22 
         Caption         =   "Check2"
         Height          =   255
         Left            =   1440
         TabIndex        =   51
         Top             =   1800
         Width           =   255
      End
      Begin VB.CheckBox Check21 
         Caption         =   "Check2"
         Height          =   255
         Left            =   1800
         TabIndex        =   50
         Top             =   1800
         Width           =   255
      End
      Begin VB.CheckBox Check20 
         Caption         =   "Check2"
         Height          =   255
         Left            =   2160
         TabIndex        =   49
         Top             =   1800
         Width           =   255
      End
      Begin VB.CheckBox Check19 
         Caption         =   "Check2"
         Height          =   255
         Left            =   2520
         TabIndex        =   48
         Top             =   1800
         Width           =   255
      End
      Begin VB.CheckBox Check18 
         Caption         =   "Check2"
         Height          =   255
         Left            =   2880
         TabIndex        =   47
         Top             =   1800
         Width           =   255
      End
      Begin VB.CheckBox Check17 
         Caption         =   "Check2"
         Height          =   255
         Left            =   3240
         TabIndex        =   46
         Top             =   1800
         Width           =   255
      End
      Begin VB.Label Label13 
         Caption         =   "AinB"
         Height          =   255
         Left            =   120
         TabIndex        =   60
         Top             =   1080
         Width           =   495
      End
      Begin VB.Label Label12 
         Caption         =   "AinA"
         Height          =   255
         Left            =   120
         TabIndex        =   59
         Top             =   480
         Width           =   495
      End
      Begin VB.Label Label9 
         Caption         =   "Din"
         Height          =   255
         Left            =   240
         TabIndex        =   58
         Top             =   1800
         Width           =   375
      End
   End
   Begin VB.CommandButton Command1 
      Caption         =   "INIT SIOS"
      Height          =   375
      Left            =   7320
      TabIndex        =   44
      Top             =   4920
      Width           =   3255
   End
   Begin VB.OptionButton Option4 
      Appearance      =   0  '2D
      Caption         =   "COM4"
      ForeColor       =   &H80000008&
      Height          =   255
      Left            =   3840
      TabIndex        =   43
      Top             =   6720
      Width           =   1095
   End
   Begin VB.OptionButton Option3 
      Appearance      =   0  '2D
      Caption         =   "COM3"
      ForeColor       =   &H80000008&
      Height          =   255
      Left            =   2520
      TabIndex        =   42
      Top             =   6720
      Width           =   1095
   End
   Begin VB.TextBox Text5 
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   13.5
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   435
      Left            =   5040
      TabIndex        =   36
      Top             =   3120
      Width           =   1095
   End
   Begin VB.Frame Frame2 
      Caption         =   "SIOS Outputs"
      Height          =   2295
      Left            =   120
      TabIndex        =   24
      Top             =   4080
      Width           =   6495
      Begin VB.TextBox Text8 
         BeginProperty Font 
            Name            =   "MS Sans Serif"
            Size            =   13.5
            Charset         =   0
            Weight          =   700
            Underline       =   0   'False
            Italic          =   0   'False
            Strikethrough   =   0   'False
         EndProperty
         Height          =   435
         Left            =   4920
         TabIndex        =   40
         Text            =   "0"
         Top             =   1680
         Width           =   1095
      End
      Begin VB.TextBox Text7 
         BeginProperty Font 
            Name            =   "MS Sans Serif"
            Size            =   13.5
            Charset         =   0
            Weight          =   700
            Underline       =   0   'False
            Italic          =   0   'False
            Strikethrough   =   0   'False
         EndProperty
         Height          =   435
         Left            =   4920
         TabIndex        =   39
         Text            =   "0 V"
         Top             =   960
         Width           =   1095
      End
      Begin VB.TextBox Text6 
         BeginProperty Font 
            Name            =   "MS Sans Serif"
            Size            =   13.5
            Charset         =   0
            Weight          =   700
            Underline       =   0   'False
            Italic          =   0   'False
            Strikethrough   =   0   'False
         EndProperty
         Height          =   435
         Left            =   4920
         TabIndex        =   37
         Text            =   "0 V"
         Top             =   360
         Width           =   1095
      End
      Begin VB.CheckBox Check1 
         Caption         =   "Check1"
         Height          =   255
         Left            =   4320
         TabIndex        =   34
         Top             =   1800
         Width           =   255
      End
      Begin VB.CheckBox Check2 
         Caption         =   "Check2"
         Height          =   255
         Left            =   3840
         TabIndex        =   33
         Top             =   1800
         Width           =   255
      End
      Begin VB.CheckBox Check3 
         Caption         =   "Check2"
         Height          =   255
         Left            =   3360
         TabIndex        =   32
         Top             =   1800
         Width           =   255
      End
      Begin VB.CheckBox Check4 
         Caption         =   "Check2"
         Height          =   255
         Left            =   2880
         TabIndex        =   31
         Top             =   1800
         Width           =   255
      End
      Begin VB.CheckBox Check5 
         Caption         =   "Check2"
         Height          =   255
         Left            =   2400
         TabIndex        =   30
         Top             =   1800
         Width           =   255
      End
      Begin VB.CheckBox Check6 
         Caption         =   "Check2"
         Height          =   255
         Left            =   1920
         TabIndex        =   29
         Top             =   1800
         Width           =   255
      End
      Begin VB.CheckBox Check7 
         Caption         =   "Check2"
         Height          =   255
         Left            =   1440
         TabIndex        =   28
         Top             =   1800
         Width           =   255
      End
      Begin VB.CheckBox Check8 
         Caption         =   "Check2"
         Height          =   255
         Left            =   960
         TabIndex        =   27
         Top             =   1800
         Width           =   255
      End
      Begin VB.HScrollBar HScroll2 
         Height          =   255
         LargeChange     =   10
         Left            =   840
         Max             =   255
         TabIndex        =   26
         Top             =   1080
         Width           =   3855
      End
      Begin VB.HScrollBar HScroll1 
         Height          =   255
         LargeChange     =   10
         Left            =   840
         Max             =   255
         TabIndex        =   25
         Top             =   480
         Width           =   3855
      End
      Begin VB.Label Label8 
         Caption         =   "AoutB"
         Height          =   255
         Left            =   240
         TabIndex        =   41
         Top             =   1080
         Width           =   615
      End
      Begin VB.Label Label7 
         Caption         =   "AoutA"
         Height          =   255
         Left            =   240
         TabIndex        =   38
         Top             =   480
         Width           =   615
      End
      Begin VB.Label Label3 
         Caption         =   "Dout"
         Height          =   255
         Left            =   360
         TabIndex        =   35
         Top             =   1800
         Width           =   495
      End
   End
   Begin VB.Frame Frame1 
      Caption         =   "SIOS Inputs"
      Height          =   3735
      Left            =   120
      TabIndex        =   2
      Top             =   120
      Width           =   6495
      Begin VB.CheckBox Check9 
         Caption         =   "Check2"
         Height          =   255
         Left            =   4200
         TabIndex        =   22
         Top             =   3120
         Width           =   255
      End
      Begin VB.CheckBox Check10 
         Caption         =   "Check2"
         Height          =   255
         Left            =   3720
         TabIndex        =   21
         Top             =   3120
         Width           =   255
      End
      Begin VB.CheckBox Check11 
         Caption         =   "Check2"
         Height          =   255
         Left            =   3240
         TabIndex        =   20
         Top             =   3120
         Width           =   255
      End
      Begin VB.CheckBox Check12 
         Caption         =   "Check2"
         Height          =   255
         Left            =   2760
         TabIndex        =   19
         Top             =   3120
         Width           =   255
      End
      Begin VB.CheckBox Check13 
         Caption         =   "Check2"
         Height          =   255
         Left            =   2280
         TabIndex        =   18
         Top             =   3120
         Width           =   255
      End
      Begin VB.CheckBox Check14 
         Caption         =   "Check2"
         Height          =   255
         Left            =   1800
         TabIndex        =   17
         Top             =   3120
         Width           =   255
      End
      Begin VB.CheckBox Check15 
         Caption         =   "Check2"
         Height          =   255
         Left            =   1320
         TabIndex        =   16
         Top             =   3120
         Width           =   255
      End
      Begin VB.CheckBox Check16 
         Caption         =   "Check1"
         Height          =   255
         Left            =   840
         TabIndex        =   15
         Top             =   3120
         Width           =   255
      End
      Begin VB.TextBox Text4 
         BeginProperty Font 
            Name            =   "MS Sans Serif"
            Size            =   13.5
            Charset         =   0
            Weight          =   700
            Underline       =   0   'False
            Italic          =   0   'False
            Strikethrough   =   0   'False
         EndProperty
         Height          =   435
         Left            =   4920
         TabIndex        =   12
         Top             =   2160
         Width           =   1335
      End
      Begin VB.TextBox Text3 
         BeginProperty Font 
            Name            =   "MS Sans Serif"
            Size            =   13.5
            Charset         =   0
            Weight          =   700
            Underline       =   0   'False
            Italic          =   0   'False
            Strikethrough   =   0   'False
         EndProperty
         Height          =   435
         Left            =   4920
         TabIndex        =   11
         Top             =   1560
         Width           =   1335
      End
      Begin VB.PictureBox Picture4 
         Height          =   255
         Left            =   720
         ScaleHeight     =   195
         ScaleWidth      =   3915
         TabIndex        =   10
         Top             =   2280
         Width           =   3975
         Begin VB.Shape Shape4 
            FillStyle       =   0  'Ausgefüllt
            Height          =   255
            Left            =   0
            Top             =   0
            Width           =   75
         End
      End
      Begin VB.PictureBox Picture3 
         Height          =   255
         Left            =   720
         ScaleHeight     =   195
         ScaleWidth      =   3915
         TabIndex        =   9
         Top             =   1680
         Width           =   3975
         Begin VB.Shape Shape3 
            FillStyle       =   0  'Ausgefüllt
            Height          =   255
            Left            =   0
            Top             =   0
            Width           =   75
         End
      End
      Begin VB.PictureBox Picture1 
         Height          =   255
         Left            =   720
         ScaleHeight     =   195
         ScaleWidth      =   3915
         TabIndex        =   6
         Top             =   480
         Width           =   3975
         Begin VB.Shape Shape1 
            FillStyle       =   0  'Ausgefüllt
            Height          =   255
            Left            =   0
            Top             =   0
            Width           =   75
         End
      End
      Begin VB.TextBox Text1 
         BeginProperty Font 
            Name            =   "MS Sans Serif"
            Size            =   13.5
            Charset         =   0
            Weight          =   700
            Underline       =   0   'False
            Italic          =   0   'False
            Strikethrough   =   0   'False
         EndProperty
         Height          =   435
         Left            =   4920
         TabIndex        =   5
         Top             =   360
         Width           =   1335
      End
      Begin VB.PictureBox Picture2 
         Height          =   255
         Left            =   720
         ScaleHeight     =   195
         ScaleWidth      =   3915
         TabIndex        =   4
         Top             =   1080
         Width           =   3975
         Begin VB.Shape Shape2 
            FillStyle       =   0  'Ausgefüllt
            Height          =   255
            Left            =   0
            Top             =   0
            Width           =   75
         End
      End
      Begin VB.TextBox Text2 
         BeginProperty Font 
            Name            =   "MS Sans Serif"
            Size            =   13.5
            Charset         =   0
            Weight          =   700
            Underline       =   0   'False
            Italic          =   0   'False
            Strikethrough   =   0   'False
         EndProperty
         Height          =   435
         Left            =   4920
         TabIndex        =   3
         Top             =   960
         Width           =   1335
      End
      Begin VB.Label Label4 
         Caption         =   "Din"
         Height          =   255
         Left            =   240
         TabIndex        =   23
         Top             =   3120
         Width           =   375
      End
      Begin VB.Label Label6 
         Caption         =   "AinD"
         Height          =   255
         Left            =   120
         TabIndex        =   14
         Top             =   2280
         Width           =   495
      End
      Begin VB.Label Label5 
         Caption         =   "AinC"
         Height          =   255
         Left            =   120
         TabIndex        =   13
         Top             =   1680
         Width           =   495
      End
      Begin VB.Label Label1 
         Caption         =   "AinA"
         Height          =   255
         Left            =   120
         TabIndex        =   8
         Top             =   480
         Width           =   495
      End
      Begin VB.Label Label2 
         Caption         =   "AinB"
         Height          =   255
         Left            =   120
         TabIndex        =   7
         Top             =   1080
         Width           =   495
      End
   End
   Begin VB.OptionButton Option1 
      Appearance      =   0  '2D
      Caption         =   "COM1"
      ForeColor       =   &H80000008&
      Height          =   255
      Left            =   120
      TabIndex        =   0
      Top             =   6720
      Value           =   -1  'True
      Width           =   1095
   End
   Begin VB.OptionButton Option2 
      Appearance      =   0  '2D
      Caption         =   "COM2"
      ForeColor       =   &H80000008&
      Height          =   255
      Left            =   1320
      TabIndex        =   1
      Top             =   6720
      Width           =   1095
   End
   Begin VB.Timer Timer1 
      Enabled         =   0   'False
      Interval        =   500
      Left            =   7440
      Top             =   6720
   End
   Begin VB.Label Label10 
      Caption         =   "Label10"
      Height          =   375
      Left            =   8280
      TabIndex        =   73
      Top             =   6360
      Width           =   1695
   End
End
Attribute VB_Name = "SIOSLAB"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Private Sub Command1_Click()
  Timer2.Enabled = False
  SENDBYTE 100
  SENDBYTE 27
  SENDBYTE 3
  SENDBYTE 255  'EEPROM adr 1023
  SENDBYTE 0
  DELAY 50
  SENDBYTE 102
  SENDBYTE 27
  SENDBYTE 0
  Interface_Init
End Sub

Private Sub Command2_Click()
  Timer1.Enabled = False
  Timer2.Enabled = False
  SENDBYTE 100
  SENDBYTE 27
  SENDBYTE 3
  SENDBYTE 255  'EEPROM adr 1023
  SENDBYTE 1
  DELAY 50
  SENDBYTE 102
  SENDBYTE 27
  SENDBYTE 1
  Interface_Init
End Sub

Private Sub Form_Load()
 i = OPENCOM("COM1:19200,N,8,1")
 If i = 0 Then
    i = OPENCOM("COM2:19200,N,8,1")
    Option2.Value = True
 End If
 TIMEINIT
  TIMEOUTS (300)
 If i = 0 Then MsgBox ("COM Error")
 Interface_Init
End Sub

Private Sub Form_Unload(Cancel As Integer)
  CLOSECOM
End Sub

Sub Interface_Init()
  SENDBYTE 0
  SENDBYTE 0
  SENDBYTE 0
  CLEARBUFFER
  SENDBYTE 1
  d = READBYTE
  If d = 10 Then
    Label10.Caption = "SIOS aktiv"
    Timer1.Enabled = True
    Timer2.Enabled = False
    Frame1.Enabled = True
    Frame2.Enabled = True
    Frame3.Enabled = False
    Frame4.Enabled = False
    SENDBYTE 83  '2,5V
    SENDBYTE 81
    SENDBYTE 1 ' Kanal1 Dev1-0*200
    SENDBYTE 9
    
  End If
  If d = 201 Then
    Label10.Caption = "CompuLAB aktiv"
    Timer1.Enabled = False
    Timer2.Enabled = True
    Frame1.Enabled = False
    Frame2.Enabled = False
    Frame3.Enabled = True
    Frame4.Enabled = True
  End If
  
  
End Sub

Private Sub HScroll1_Change()
  Data = HScroll1.Value
  Text6.Text = Str$(Data * 0.02) + " V"
  AoutA Data
End Sub

Private Sub HScroll2_Change()
  Data = HScroll2.Value
  Text7.Text = Str$(Data * 0.02) + " V"
  AoutB Data
End Sub

Private Sub Option1_Click()
 CLOSECOM
 i = OPENCOM("COM1:19200,N,8,1")
 If i = 0 Then MsgBox ("COM1 Error")
 TIMEOUTS (300)
End Sub

Private Sub Option2_Click()
 CLOSECOM
 i = OPENCOM("COM2:19200,N,8,1")
 If i = 0 Then MsgBox ("COM2 Error")
 TIMEOUTS (300)
End Sub

Private Sub Dout(Data)
  SENDBYTE 16
  SENDBYTE Data
End Sub

Private Function Din()
  SENDBYTE 32
  Din = READBYTE
End Function

Private Function AinA()
  SENDBYTE 48
  AinA = READBYTE
End Function

Private Function AinB()
  SENDBYTE 49
  AinB = READBYTE
End Function

Private Function AinC()
  SENDBYTE 50
  AinC = READBYTE
End Function

Private Function AinD()
  SENDBYTE 51
  AinD = READBYTE
End Function

Private Function AinA10Bit()
  SENDBYTE 56
  Hi = READBYTE
  SENDBYTE 1
  Lo = READBYTE
  u = Hi * 256 + Lo
  If u < 0 Then u = 0
  AinA10Bit = u
End Function

Private Function AinB10Bit()
  SENDBYTE 57
  Hi = READBYTE
  SENDBYTE 1
  Lo = READBYTE
  u = Hi * 256 + Lo
  If u < 0 Then u = 0
  AinB10Bit = u
End Function

Private Function AinC10Bit()
  SENDBYTE 58
  Hi = READBYTE
  SENDBYTE 1
  Lo = READBYTE
  u = Hi * 256 + Lo
  If u < 0 Then u = 0
  AinC10Bit = u
End Function

Private Function AinD10Bit()
  SENDBYTE 59
  Hi = READBYTE
  SENDBYTE 1
  Lo = READBYTE
  u = Hi * 256 + Lo
  If u < 0 Then u = 0
  AinD10Bit = u
End Function

Private Sub AoutA(Data)
  SENDBYTE 64
  SENDBYTE Data
End Sub

Private Sub AoutB(Data)
  SENDBYTE 65
  SENDBYTE Data
End Sub


Private Sub Option3_Click()
 CLOSECOM
 i = OPENCOM("COM3:19200,N,8,1")
 If i = 0 Then MsgBox ("COM3 Error")
 TIMEOUTS (300)
End Sub

Private Sub Option4_Click()
 CLOSECOM
 i = OPENCOM("COM4:19200,N,8,1")
 If i = 0 Then MsgBox ("COM4 Error")
 TIMEOUTS (300)
End Sub

Private Sub Option5_Click()
 OpenString = "COM" + Text13.Text + ":19200,N,8,1"
 CLOSECOM
 i = OPENCOM(OpenString)
 If i = 0 Then MsgBox ("COMx Error")
 TIMEOUTS (300)
End Sub

Private Sub Timer1_Timer()
Data = 0
  Data = Data + Check1.Value
  Data = Data + Check2.Value * 2
  Data = Data + Check3.Value * 4
  Data = Data + Check4.Value * 8
  Data = Data + Check5.Value * 16
  Data = Data + Check6.Value * 32
  Data = Data + Check7.Value * 64
  Data = Data + Check8.Value * 128
  Dout Data
  Text8.Text = Str$(Data)
  CLEARBUFFER
  Ua = AinA10Bit
  Shape1.Width = 50 + Ua / 4 * 15
  Text1.Text = Str$(Ua * 0.005) + " V"
  Ub = AinB10Bit
  Shape2.Width = 50 + Ub / 4 * 15
  Text2.Text = Str$(Ub * 0.005) + " V"
  Uc = AinC10Bit
  Shape3.Width = 50 + Uc / 4 * 15
  Text3.Text = Str$(Uc * 0.005) + " V"
  Ud = AinD10Bit
  Shape4.Width = 50 + Ud / 4 * 15
  Text4.Text = Str$(Ud * 0.005) + " V"
Data = Din
  Text5.Text = Str$(Data)
  Check9.Value = Data And 1
  Check10.Value = (Data And 2) \ 2
  Check11.Value = (Data And 4) \ 4
  Check12.Value = (Data And 8) \ 8
  Check13.Value = (Data And 16) \ 16
  Check14.Value = (Data And 32) \ 32
  Check15.Value = (Data And 64) \ 64
  Check16.Value = (Data And 128) \ 128
End Sub

Private Sub Timer2_Timer()
  Data = 0
  Data = Data + Check25.Value
  Data = Data + Check26.Value * 2
  Data = Data + Check27.Value * 4
  Data = Data + Check28.Value * 8
  Data = Data + Check29.Value * 16
  Data = Data + Check30.Value * 32
  Data = Data + Check31.Value * 64
  Data = Data + Check32.Value * 128
  Text9.Text = Str(Data)
  SENDBYTE 81
  SENDBYTE Data
  CLEARBUFFER
  SENDBYTE 60
  Ua = READBYTE()
  Shape7.Width = 50 + Ua / 4 * 15
  Text11.Text = Str$(Ua * 0.02) + " V"
  SENDBYTE 58
  Ub = READBYTE()
  Shape8.Width = 50 + Ub / 4 * 15
  Text12.Text = Str$(Ub * 0.02) + " V"
  
  SENDBYTE 211
  Data = READBYTE()
  Text10.Text = Str$(Data)
  Check17.Value = Data And 1
  Check18.Value = (Data And 2) \ 2
  Check19.Value = (Data And 4) \ 4
  Check20.Value = (Data And 8) \ 8
  Check21.Value = (Data And 16) \ 16
  Check22.Value = (Data And 32) \ 32
  Check23.Value = (Data And 64) \ 64
  Check24.Value = (Data And 128) \ 128

End Sub
