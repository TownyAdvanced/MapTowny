#
# Copyright (c) 2022 Silverwolfg11
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

# Get absolute path of home directory
HOME_PATH="$(readlink -f ~)"
# Create M2 home path
M2_HOME="$HOME_PATH/.m2"
# Create .m2 folder if it doesn't exist
mkdir -p "$M2_HOME"

JDK_8_HOME="$(cat "$M2_HOME/jdk_8_path")"
# Copy toolchain file to m2 directory
cp ./jitpack/jitpack_toolchain.xml "$M2_HOME/toolchains.xml"
# Replace JDK 8 prefix with actual jdk8 home
sed -i "s+{JAVA_8_SDK}+$JDK_8_HOME+" "$M2_HOME/toolchains.xml"

