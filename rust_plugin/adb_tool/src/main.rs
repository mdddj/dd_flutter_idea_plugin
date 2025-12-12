use clap::Parser;
use forensic_adb::{AndroidStorageInput, DeviceError, Host};
use serde::Serialize;
use std::collections::BTreeMap;

#[derive(Serialize, Debug)]
struct DeviceInfo {
    serial: String,
    info: BTreeMap<String, String>,
}

#[derive(Parser, Debug)]
#[command(author, version, about, long_about = None)]
struct Args {
    /// Optional device serial number
    #[arg(short, long)]
    serial: Option<String>,

    /// The command to execute
    command: Option<String>,

    /// Argument for the command
    #[arg(long)]
    arg: Option<String>,
}

async fn list_android_devices() -> Result<(), DeviceError> {
    let host = Host::default();
    let devices = host.devices::<Vec<forensic_adb::DeviceInfo>>().await?;

    if devices.is_empty() {
        println!("No devices found.");
    } else {
        for device in devices {
            let device_info = DeviceInfo {
                serial: device.serial.to_string(),
                info: device.info,
            };
            println!("{}", serde_json::to_string(&device_info).unwrap());
        }
    }
    Ok(())
}

async fn execute_adb_command(
    serial: Option<&String>,
    command: &str,
    arg: Option<&str>,
) -> Result<(), DeviceError> {
    let host = Host::default();

    let device = host
        .device_or_default(serial.as_ref(), AndroidStorageInput::default())
        .await?;

    println!(
        "Selected device: {:?}. Executing command: {}",
        device, command
    );

    match command {
        "install" => {
            if let Some(apk_path) = arg {
                println!("Installing APK: {} on device: {:?}...", apk_path, device);
                let install_command = format!("pm install {}", apk_path);
                let output = device.execute_host_shell_command(&install_command).await?;
                println!("APK installation output:\n{}", output);
            } else {
                eprintln!("Error: 'install' command requires an APK path.");
            }
        }
        _ => {
            let output = device.execute_host_shell_command(command).await?;
            println!("Command output:\n{}", output);
        }
    }

    Ok(())
}

#[tokio::main]
async fn main() -> Result<(), DeviceError> {
    let args = Args::parse();

    match args.command {
        Some(cmd) if cmd == "list" => {
            list_android_devices().await?;
        }
        Some(cmd) => {
            execute_adb_command(args.serial.as_ref(), &cmd, args.arg.as_deref()).await?;
        }
        None => {
            list_android_devices().await?;
        }
    }

    Ok(())
}
